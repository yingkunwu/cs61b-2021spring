package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

public class Repo extends Repository {
    /** This function get the file content tracked by the specific commit and add to the current directory. */
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
    public static boolean untrackedFileExist(Set<String> filesSet, String checkoutCommitID) {
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);
        TreeMap<String, String> checkoutCommitTree = getCommitTreeWithCommitID(checkoutCommitID);

        for (String file : filesSet) {
            if (filesToBeIgnored.contains(file)) continue;
            if (!latestCommitTree.containsKey(file) && checkoutCommitTree.containsKey(file)) {
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
        if (untrackedFileExist(filesSet, checkoutCommitID)) {
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
            /* Takes the version of the file as it exists in the commit with the given id, and puts it in the
            working directory, overwriting the version of the file that’s already there if there is one. The new
            version of the file is not staged.
             */
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
            /* Takes all files in the commit at the head of the given branch, and puts them in the working
            directory, overwriting the versions of the files that are already there if they exist. Also, at the end
            of this command, the given branch will now be considered the current branch (HEAD). Any files that are
            tracked in the current branch but are not present in the checked-out branch are deleted. The staging
            area is cleared, unless the checked-out branch is the current branch.
             */
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
            /* Takes the version of the file as it exists in the head commit and puts it in the working
            directory, overwriting the version of the file that’s already there if there is one. The new
            version of the file is not staged.
             */
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

    /*public static String findSplitCommit(String commitID1, String commitID2) {
        Queue<String> parent1 = new LinkedList<>(List.of(commitID1));
        Queue<String> parent2 = new LinkedList<>(List.of(commitID2));

        while (!parent1.isEmpty()) {
            if (parent1.peek().length() == 0) {
                parent1.remove();
                continue;
            }
            while (!parent2.isEmpty()) {
                if (parent2.peek().length() == 0) {
                    parent2.remove();
                    continue;
                }
                if (Objects.equals(parent1.peek(), parent2.peek())) {
                    return parent1.peek();
                }
                Commit branchCommit = readObject(join(OBJECT_DIR, parent2.peek()), Commit.class);
                parent2.addAll(branchCommit.getParent());
                parent2.remove();
            }
            Commit branchCommit = readObject(join(OBJECT_DIR, parent1.peek()), Commit.class);
            parent1.addAll(branchCommit.getParent());
            parent1.remove();
            parent2 = new LinkedList<>(List.of(commitID2));
        }
        return null;
    }*/

    public static void loadParent(String commitID, HashSet<String> allParents) {
        if (commitID.length() == 0) {
            return;
        }
        allParents.add(commitID);
        Commit commit = readObject(join(OBJECT_DIR, commitID), Commit.class);
        ArrayList<String> parents = commit.getParent();
        for (String p : parents) {
            loadParent(p, allParents);
        }
    }

    /** Find the closest shared parent from two commits */
    public static String findSplitCommit(String commitID1, String commitID2) {
        Queue<String> parent1 = new LinkedList<>(List.of(commitID1));
        HashSet<String> parent2 = new HashSet<>();
        loadParent(commitID2, parent2);

        if (parent2.contains(parent1.peek())) {
            return parent1.peek();
        }

        while (!parent1.isEmpty()) {
            if (parent1.peek().length() == 0) {
                parent1.remove();
                continue;
            }
            Commit branchCommit = readObject(join(OBJECT_DIR, parent1.peek()), Commit.class);
            ArrayList<String> parents = branchCommit.getParent();
            String p1 = parents.get(0);
            if (parent2.contains(p1)) {
                return p1;
            } else {
                parent1.add(p1);
            }
            if (parents.size() > 1) {
                String p2 = parents.get(1);
                if (parent2.contains(p2)) {
                    return p2;
                } else {
                    parent1.add(p2);
                }
            }
            parent1.remove();
        }
        return null;
    }

    /** Solve merge conflict files by keeping contents in both files */
    public static String solveMergeConflict(String filename, String currentBlobUID, String branchBlobUID) {
        String currentContent = "";
        String branchContent = "";

        if (currentBlobUID != null)  {
            Blob currentBlob = readObject(join(OBJECT_DIR, currentBlobUID), Blob.class);
            currentContent = currentBlob.getContent();
        }
        if (branchBlobUID != null) {
            Blob branchBlob = readObject(join(OBJECT_DIR, branchBlobUID), Blob.class);
            branchContent = branchBlob.getContent();
        }

        String mergeContent = "<<<<<<< HEAD\n" + currentContent + "=======\n" + branchContent + ">>>>>>>\n";
        Blob blob = new Blob(mergeContent);
        String blobUID = blob.Hash();
        writeObject(join(OBJECT_DIR, blobUID), blob);
        writeContents(join(CWD, filename), mergeContent);

        return blobUID;
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
        if (untrackedFileExist(filesSet, branchCommitID)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }

        // Find the split commit
        String splitCommitID = findSplitCommit(currentCommitID, branchCommitID);
        if (splitCommitID == null) throw new java.lang.Error("Split not found");

        if (splitCommitID.equals(branchCommitID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitCommitID.equals(currentCommitID)) {
            checkoutToSpecificCommitID(branchCommitID, branch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        TreeMap<String, String> currentTree = getCommitTreeWithCommitID(currentCommitID);
        TreeMap<String, String> branchTree = getCommitTreeWithCommitID(branchCommitID);
        TreeMap<String, String> splitTree = getCommitTreeWithCommitID(splitCommitID);

        TreeMap<String, String> tree = new TreeMap<>();

        // Conditions when the file is tracked by split
        for (Map.Entry<String, String> entry : splitTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            String currentBlobUID = currentTree.get(filename);
            String branchBlobUID = branchTree.get(filename);

            // modified in other but not head -> other
            if (Objects.equals(currentBlobUID, blobUID) && !Objects.equals(branchBlobUID, blobUID)) {
                /* Any files present at the split point, unmodified in the current branch, and absent in the
                given branch should be removed (and untracked).
                 */
                if (branchBlobUID == null) {
                    deleteFile(join(CWD, filename));
                } else {
                    /* Any files that have been modified in the given branch since the split point, but not
                    modified in the current branch since the split point should be changed to their versions
                    in the given branch (checked out from the commit at the front of the given branch).
                     */
                    replaceFileWithCommitID(branchCommitID, filename);
                    tree.put(filename, branchBlobUID);
                }
                // modified in head but not other -> head
            } else if (!Objects.equals(currentBlobUID, blobUID) && Objects.equals(branchBlobUID, blobUID)) {
                /* Any files present at the split point, unmodified in the given branch, and absent in the
                current branch should remain absent.
                 */
                if (currentBlobUID != null) {
                    /* Any files that have been modified in the current branch but not in the given branch
                    since the split point should stay as they are.
                     */
                    tree.put(filename, currentBlobUID);
                }
                // modified in both
            } else if (!Objects.equals(currentBlobUID, blobUID) && !Objects.equals(branchBlobUID, blobUID)) {
                // if the file is not absent in both commit
                if (!(currentBlobUID == null) || !(branchBlobUID == null)) {
                    // modified in the same way
                    if (Objects.equals(currentBlobUID, branchBlobUID)) {
                        tree.put(filename, currentBlobUID);
                    } else {
                        // modified in the different way
                        System.out.println("Encountered a merge conflict.");
                        String newBlobUID = solveMergeConflict(filename, currentBlobUID, branchBlobUID);
                        tree.put(filename, newBlobUID);
                    }
                }
                // nothing changed
            } else {
                tree.put(filename, currentBlobUID); // Doesn't matter
            }
        }

        // Conditions when the file is not tracked by split
        /* Any files that were not present at the split point and are present only
        in the current branch should remain as they are.
         */
        for (Map.Entry<String, String> entry : currentTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (!splitTree.containsKey(filename)) {
                if (branchTree.containsKey(filename)) {
                    String branchBlobUID = branchTree.get(filename);
                    if (!Objects.equals(blobUID, branchBlobUID)) {
                        /* If the file was absent at the split point and has different contents in the
                        given and current branches, the conflict happens.
                         */
                        System.out.println("Encountered a merge conflict.");
                        String newBlobUID = solveMergeConflict(filename, blobUID, branchBlobUID);
                        tree.put(filename, newBlobUID);
                    } else {
                        /* If the file was absent at the split point and has same contents in the
                        given and current branches, they remain as they are.
                         */
                        tree.put(filename, blobUID);
                    }
                } else {
                    tree.put(filename, blobUID);
                }
            }
        }

        /* Any files that were not present at the split point and are present only
        in the given branch should be checked out and staged.
         */
        for (Map.Entry<String, String> entry : branchTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (!splitTree.containsKey(filename) && !currentTree.containsKey(filename)) {
                replaceFileWithCommitID(branchCommitID, filename);
                tree.put(filename, blobUID);
            }
        }

        String message = "Merged " + branch + " into " + currentBranch + ".";
        ArrayList<String> parent = new ArrayList<>(Arrays.asList(currentCommitID, branchCommitID));
        doCommit(message, parent, tree);
    }
}
