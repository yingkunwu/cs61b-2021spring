package gitlet;


import java.io.File;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Utils.readContentsAsString;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECT_DIR = join(GITLET_DIR, "object");
    public static final File HEAD_DIR = join(GITLET_DIR, "HEAD");
    public static final File BRANCH_DIR = join(GITLET_DIR, "BRANCH");
    public static final File TREE_DIR = join(GITLET_DIR, "TREE");
    public static final Set<String> filesToBeIgnored = new HashSet<>(Arrays.asList(".DS_Store", "Makefile", "gitlet-design.md", "pom.xml"));

    public static String getHeadCommitID() {
        String currentBranch = readContentsAsString(HEAD_DIR);
        return readContentsAsString(join(BRANCH_DIR, currentBranch));
    }

    public static TreeMap<String, String> getCommitTreeWithCommitID(String commitID) {
        Commit latestCommit = readObject(join(OBJECT_DIR, commitID), Commit.class);
        return latestCommit.getTree();
    }

    public static void doCommit(String message, String parent, TreeMap<String, String> tree) {
        // Generate commit object
        Commit commit = new Commit(message, parent, tree);
        String UID = commit.Hash();
        writeObject(join(OBJECT_DIR, UID), commit);

        // Update HEAD and Master
        String currentBranch = readContentsAsString(HEAD_DIR);
        writeContents(join(BRANCH_DIR, currentBranch), UID);
    }

    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    public static void init() {
        if (!GITLET_DIR.mkdir()) {
            throw new java.lang.Error("Cannot create gitlet directory");
        }
        if (!OBJECT_DIR.mkdir()) {
            throw new java.lang.Error("Cannot create object directory");
        }
        if (!BRANCH_DIR.mkdir()) {
            throw new java.lang.Error("Cannot create branch directory");
        }

        writeContents(HEAD_DIR, "master");
        Stage stage = new Stage();
        writeObject(TREE_DIR, stage);
        doCommit("initial commit", "", new TreeMap<>());
    }

    public static void add(String filename) {
        // Generate glob and store it no matter what
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Blob blob = new Blob(file);
        String blobUID = blob.Hash();
        writeObject(join(Repository.OBJECT_DIR, blobUID), blob);

        // Retrieve previous stage status
        Stage stage = readObject(TREE_DIR, Stage.class);
        stage.addAddition(filename, blobUID);

        // Retrieve the latest commit tree
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);

        // Remove the addition for stage if the status of the file is the same as the tracked status
        if (Objects.equals(latestCommitTree.get(filename), blobUID)) {
            stage.removeAddition(filename);
        }

        // Store it in staging area
        writeObject(TREE_DIR, stage);
    }

    public static void commit(String message) {
        Stage stage = readObject(TREE_DIR, Stage.class);
        if (stage.empty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // Retrieve current addition and removal
        TreeMap<String, String> additionTree = stage.getAddition();
        ArrayList<String> removalList = stage.getRemoval();
        HashSet<String> removalSet = new HashSet<>(removalList);

        // Retrieve the latest commit tree
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);

        // Update current addition
        if (latestCommitTree != null) {
            for (Map.Entry<String, String> entry : latestCommitTree.entrySet()) {
                String filename = entry.getKey();
                String blobUID = entry.getValue();

                // Link the previous tracked status, ignore it if it is updated in current commit
                if (!additionTree.containsKey(filename) && !removalSet.contains(filename)) {
                    additionTree.put(filename, blobUID);
                }
            }
        }

        // Create new commit, initialize stage status, and store the stage status
        doCommit(message, HEAD, additionTree);
        stage.initialize();
        writeObject(TREE_DIR, stage);
    }

    public static void rm(String filename) {
        File file = join(CWD, filename);

        // Retrieve the latest commit tree and the stage status
        Stage stage = readObject(TREE_DIR, Stage.class);
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);

        if (latestCommitTree.containsKey(filename)) {
            // TODO: If the file is tracked in the current commit, stage it for removal and remove the file from
            //  the working directory if the user has not already done so
            stage.addRemoval(filename);
            deleteFile(file);
        } else if (stage.exist(filename)) {
            // TODO: Unstage the file if it is currently staged for addition.
            stage.removeAddition(filename);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // Store the stage status
        writeObject(TREE_DIR, stage);
    }

    public static void log() {
        // Retrieve previous commit
        String HEAD = getHeadCommitID();
        while (HEAD.length() > 0) {
            Commit commit = readObject(join(OBJECT_DIR, HEAD), Commit.class);
            String date = commit.getTimestamp();
            String message = commit.getMessage();

            System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", HEAD, date, message);
            HEAD = commit.getParent();
        }
    }

    public static void globalLog() {
        List<String> fileList = plainFilenamesIn(OBJECT_DIR);
        for (String file : fileList) {
            try {
                Commit commit = readObject(join(OBJECT_DIR, file), Commit.class);
                String date = commit.getTimestamp();
                String message = commit.getMessage();

                System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", file, date, message);
            } catch (Exception ignored) {}
        }
    }

    public static void find(String messageToFind) {
        List<String> fileList = plainFilenamesIn(OBJECT_DIR);
        boolean flag = false;
        for (String file : fileList) {
            try {
                Commit commit = readObject(join(OBJECT_DIR, file), Commit.class);
                String message = commit.getMessage();

                if (message.contains(messageToFind)) {
                    System.out.println(file);
                    flag = true;
                }
            } catch (Exception ignored) {}
        }
        if (!flag) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void status() {
        // Retrieve branches
        List<String> branches = plainFilenamesIn(BRANCH_DIR); // List is already sorted in plainFilenamesIn

        // Retrieve current stage
        Stage stage = readObject(TREE_DIR, Stage.class);
        TreeMap<String, String> additionTree = stage.getAddition(); // TreeMap is already sorted
        ArrayList<String> removalList = stage.getRemoval();
        HashSet<String> removalSet = new HashSet<>(removalList);
        Collections.sort(removalList);

        // Retrieve the latest commit tree
        String HEAD = getHeadCommitID();
        TreeMap<String, String> latestCommitTree = getCommitTreeWithCommitID(HEAD);

        // Retrieve all the file name in the directory
        List<String> files = plainFilenamesIn(CWD);
        Set<String> filesSet = new HashSet<>(files);

        // List branches
        System.out.println("=== Branches ===");
        for (String branch : branches) {
            if (Objects.equals(readContentsAsString(join(BRANCH_DIR, branch)), HEAD)) {
                branch = "*" + branch;
            }
            System.out.println(branch);
        }
        System.out.println();

        // List staged files
        System.out.println("=== Staged Files ===");
        for (Map.Entry<String, String> entry : additionTree.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();

        // List removed files
        System.out.println("=== Removed Files ===");
        for (String filename : removalList) {
            System.out.println(filename);
        }
        System.out.println();

        // List Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (Map.Entry<String, String> entry : latestCommitTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (filesSet.contains(filename)) {
                Blob blob = new Blob(join(CWD, filename));
                String fileBlobUID = blob.Hash();
                if ((!Objects.equals(fileBlobUID, blobUID))) {
                    if (!additionTree.containsKey(filename)) {
                        // TODO: Tracked in the current commit, changed in the working directory, but not staged
                        System.out.println(filename + " (modified)");
                    } else {
                        // TODO: Staged for addition, but with different contents than in the working directory
                        String stageBlobUID = additionTree.get(filename);
                        if (!Objects.equals(fileBlobUID, stageBlobUID)) {
                            System.out.println(filename + " (modified)");
                        }
                    }
                }
            } else {
                // TODO: Not staged for removal, but tracked in the current commit and deleted from the working directory
                if (!removalSet.contains(filename)) {
                    System.out.println(filename + " (deleted)");
                }
            }
        }
        // TODO: Staged for addition, but deleted in the working directory
        for (Map.Entry<String, String> entry : additionTree.entrySet()) {
            String filename = entry.getKey();
            if (!filesSet.contains(filename)) {
                System.out.println(filename + " (modified)");
            }
        }
        System.out.println();

        // TODO: files present in the working directory but neither staged for addition nor tracked
        System.out.println("=== Untracked Files ===");
        for (String filename : filesSet) {
            if (filesToBeIgnored.contains(filename)) continue;
            if (!latestCommitTree.containsKey(filename) && !additionTree.containsKey(filename)) {
                System.out.println(filename);
            }
        }
        System.out.println();
    }
}
