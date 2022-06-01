package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

public class Repo extends Repository {

    public static void replaceFileWithCommitID(String commitID, String filename) {
        TreeMap<String, String> tree = getCommitTreeWithCommitID(commitID);
        if (!tree.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobUID = tree.get(filename);
        Blob blob = readObject(join(OBJECT_DIR, blobUID), Blob.class);
        writeContents(join(CWD, filename), blob.getContent());
    }

    /** A tracked file is defined as being tracked by the current commit tree and have the exact same content. */
    public static boolean isUntrackedFileExist(Set<String> filesSet, String checkoutCommitID) {
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);
        TreeMap<String, String> checkoutCommitTree = getCommitTreeWithCommitID(checkoutCommitID);

        for (String file : filesSet) {
            if (filesToBeIgnored.contains(file)) continue;
            if (!latestCommitTree.containsKey(file)) {
                return true;
            }
        }
        return false;
    }

    /** Add or overwritten files in the current directory from the checkout commit,
     * and delete files that are not in the checkout commit */
    public static void checkoutToSpecificCommitID(String checkoutCommitID, String checkoutBranch) {
        List<String> files = plainFilenamesIn(CWD);
        Set<String> filesSet = new HashSet<>(files);

        // Find if there is any untracked file that would be overwritten or deleted by the checkout
        if (isUntrackedFileExist(filesSet, checkoutCommitID)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }

        String HEAD = getHeadCommitID();
        TreeMap<String, String> currentCommitTree = getCommitTreeWithCommitID(HEAD);
        TreeMap<String, String> checkoutCommitTree = getCommitTreeWithCommitID(checkoutCommitID);
        Stage stage = readObject(TREE_DIR, Stage.class);

        // Delete files that exist in the current branch but not exist in the checked-out branch
        for (String file : filesSet) {
            if (filesToBeIgnored.contains(file)) continue;
            if (currentCommitTree.containsKey(file) && !checkoutCommitTree.containsKey(file)) {
                deleteFile(join(CWD, file));
            }
        }

        // Takes all files in the commit at the head of the given branch, and puts them in the working directory
        for (Map.Entry<String, String> entry : checkoutCommitTree.entrySet()) {
            String branchFilename = entry.getKey();
            String branchBlobUID = entry.getValue();
            Blob blob = readObject(join(OBJECT_DIR, branchBlobUID), Blob.class);
            writeContents(join(CWD, branchFilename), blob.getContent());
        }

        // Update the current branch (point head to the checked-out branch)
        writeContents(HEAD_DIR, checkoutBranch);
        writeContents(join(BRANCH_DIR, checkoutBranch), checkoutCommitID);

        // Store the stage status
        stage.initialize();
        writeObject(TREE_DIR, stage);
    }

    public static void checkout(String filename, String commitID, String branch) {
        if (commitID != null) {
            // TODO: Takes the version of the file as it exists in the commit with the given id, and puts it in the
            //  working directory, overwriting the version of the file that’s already there if there is one. The new
            //  version of the file is not staged.
            if (commitID.length() > 40) {
                System.out.println("Commit ID should not be longer than 40 digits");
                System.exit(0);
            }
            List<String> fileList = plainFilenamesIn(OBJECT_DIR);

            // Since we may not get the whole commit ID from the user,
            // we have to iterate the list to find the one matched with the input.
            for (String file : fileList) {
                if (file.contains(commitID)) {
                    replaceFileWithCommitID(file, filename);
                    return;
                }
            }
            System.out.println("No commit with that id exists.");
            System.exit(0);

        } else if (branch != null) {
            // TODO: Takes all files in the commit at the head of the given branch, and puts them in the working
            //  directory, overwriting the versions of the files that are already there if they exist. Also, at the end
            //  of this command, the given branch will now be considered the current branch (HEAD). Any files that are
            //  tracked in the current branch but are not present in the checked-out branch are deleted. The staging
            //  area is cleared, unless the checked-out branch is the current branch.
            File branchPath = join(BRANCH_DIR, branch);
            if (!branchPath.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            String currentBranch = readContentsAsString(HEAD_DIR);
            if (Objects.equals(branch, currentBranch)) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }

            // Retrieve the latest commit of the specified branch
            String branchCommitID = readContentsAsString(branchPath);
            checkoutToSpecificCommitID(branchCommitID, branch);

        } else {
            // TODO: Takes the version of the file as it exists in the head commit and puts it in the working
            //  directory, overwriting the version of the file that’s already there if there is one. The new
            //  version of the file is not staged.
            String HEAD = getHeadCommitID();
            replaceFileWithCommitID(HEAD, filename);
        }
    }

    public static void reset(String commitID) {
        if (commitID.length() > 40) {
            System.out.println("Commit ID should not be longer than 40 digits");
            System.exit(0);
        }
        List<String> commitIDHistoryList = plainFilenamesIn(OBJECT_DIR);
        for (String commitIDHistory : commitIDHistoryList) {
            if (commitIDHistory.contains(commitID)) {
                String currentBranch = readContentsAsString(HEAD_DIR);
                checkoutToSpecificCommitID(commitIDHistory, currentBranch);
                return;
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    public static String findSplitCommit(String commitID1, String commitID2) {
        String parent1 = commitID1;
        String parent2 = commitID2;

        while (parent1.length() > 0) {
            while (parent2.length() > 0) {
                System.out.println(parent1 + " ||| " + parent2);
                if (Objects.equals(parent1, parent2)) {
                    return parent1;
                }
                Commit branchCommit = readObject(join(OBJECT_DIR, parent2), Commit.class);
                parent2 = branchCommit.getParent();
            }
            Commit commit = readObject(join(OBJECT_DIR, parent1), Commit.class);
            parent1 = commit.getParent();
            parent2 = commitID2;
        }
        return null;
    }

    public static void merge(String branch) {
        Stage stage = readObject(TREE_DIR, Stage.class);
        if (!stage.empty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        File branchPath = join(BRANCH_DIR, branch);
        if (!branchPath.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String currentBranch = readContentsAsString(HEAD_DIR);
        if (Objects.equals(branch, currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        List<String> files = plainFilenamesIn(CWD);
        Set<String> filesSet = new HashSet<>(files);
        String currentCommitID = getHeadCommitID();
        String branchCommitID = readContentsAsString(branchPath);

        // Find if there is any untracked file that would be overwritten or deleted by the checkout
        if (isUntrackedFileExist(filesSet, branchCommitID)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }

        // Find the split commit
        String splitCommitID = findSplitCommit(currentCommitID, branchCommitID);
        if (splitCommitID == null) throw new java.lang.Error("Split not found");

        if (splitCommitID.equals(currentCommitID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitCommitID.equals(branchCommitID)) {
            checkoutToSpecificCommitID(branchCommitID, branch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        TreeMap<String, String> currentTree = getCommitTreeWithCommitID(currentCommitID);
        TreeMap<String, String> branchTree = getCommitTreeWithCommitID(branchCommitID);
        TreeMap<String, String> splitTree = getCommitTreeWithCommitID(splitCommitID);

        TreeMap<String, String> tree = new TreeMap<>();
        for (Map.Entry<String, String> entry : splitTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            String currentBlobUID = currentTree.get(filename);
            String branchBlobUID = branchTree.get(filename);

                // modified in other but not head -> other
            if (Objects.equals(currentBlobUID, blobUID) && !Objects.equals(branchBlobUID, blobUID)) {
                // TODO: Any files present at the split point, unmodified in the current branch, and absent in the
                //  given branch should be removed (and untracked).
                if (branchBlobUID == null) {
                    deleteFile(join(CWD, filename));
                } else {
                    // TODO: Any files that have been modified in the given branch since the split point, but not
                    //  modified in the current branch since the split point should be changed to their versions
                    //  in the given branch (checked out from the commit at the front of the given branch).
                    replaceFileWithCommitID(branchCommitID, filename);
                    tree.put(filename, branchBlobUID);
                }
                // modified in head but not other -> head
            } else if (!Objects.equals(currentBlobUID, blobUID) && Objects.equals(branchBlobUID, blobUID)) {
                // TODO: Any files present at the split point, unmodified in the given branch, and absent in the
                //  current branch should remain absent.
                if (currentBlobUID != null) {
                    // TODO: Any files that have been modified in the current branch but not in the given branch
                    //  since the split point should stay as they are.
                    tree.put(filename, currentBlobUID);
                }
                // modified in both
            } else if (!Objects.equals(currentBlobUID, blobUID) && !Objects.equals(branchBlobUID, blobUID)) {
                    // if the file is not absent in both commit
                if (!((currentBlobUID == null) && (branchBlobUID == null))) {
                        // modified in the same way
                    if (Objects.equals(currentBlobUID, branchBlobUID)) {
                        tree.put(filename, currentBlobUID); // Doesn't matter
                    } else {
                        // modified in the different way
                        // TODO: conflict
                        System.out.println("Encountered a merge conflict.");
                    }
                }
                // nothing changed
            } else {
                tree.put(filename, currentBlobUID); // Doesn't matter
            }
        }

        // TODO: Any files that were not present at the split point and are present only
        //  in the current branch should remain as they are.
        for (Map.Entry<String, String> entry : currentTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (!splitTree.containsKey(filename) && !branchTree.containsKey(filename)) {
                tree.put(filename, blobUID);
            }
        }

        // TODO: Any files that were not present at the split point and are present only
        //  in the given branch should be checked out and staged.
        for (Map.Entry<String, String> entry : branchTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (!splitTree.containsKey(filename) && !currentTree.containsKey(filename)) {
                replaceFileWithCommitID(branchCommitID, filename);
                tree.put(filename, blobUID);
            }
        }

        String message = "Merged " + branch + " into " + currentBranch + ".";
        doCommit(message, currentCommitID, branchCommitID, tree);
    }
}
