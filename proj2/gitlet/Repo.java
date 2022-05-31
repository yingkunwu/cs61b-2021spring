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
            List<String> files = plainFilenamesIn(CWD);
            Set<String> filesSet = new HashSet<>(files);

            // Find if there is any untracked file
            String HEAD = getHeadCommitID();
            TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);
            Stage stage = readObject(TREE_DIR, Stage.class);
            TreeMap<String, String> additionTree = stage.getAddition();
            for (String file : filesSet) {
                if (filesToBeIgnored.contains(file)) continue;
                if (!latestCommitTree.containsKey(file) && !additionTree.containsKey(file)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }

            // Retrieve the latest commit of the specified branch
            String branchCommitID = readContentsAsString(branchPath);
            TreeMap<String, String> tree = getCommitTreeWithCommitID(branchCommitID);

            // Delete files that are not exist in the checked-out branch
            for (String file : filesSet) {
                if (filesToBeIgnored.contains(file)) continue;
                if (!tree.containsKey(file)) {
                    deleteFile(join(CWD, file));
                }
            }

            // Takes all files in the commit at the head of the given branch, and puts them in the working directory
            for (Map.Entry<String, String> entry : tree.entrySet()) {
                String branchFilename = entry.getKey();
                String branchBlobUID = entry.getValue();
                Blob blob = readObject(join(OBJECT_DIR, branchBlobUID), Blob.class);
                writeObject(join(CWD, branchFilename), blob.getContent());
            }

            // Update the current branch (point head to the checked-out branch)
            writeContents(HEAD_DIR, branch);

            // Store the stage status
            stage.initialize();
            writeObject(TREE_DIR, stage);

        } else {
            // TODO: Takes the version of the file as it exists in the head commit and puts it in the working
            //  directory, overwriting the version of the file that’s already there if there is one. The new
            //  version of the file is not staged.
            String HEAD = getHeadCommitID();
            replaceFileWithCommitID(HEAD, filename);
        }
    }
}
