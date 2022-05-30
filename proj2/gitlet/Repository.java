package gitlet;


import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

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

    /* TODO: fill in the rest of this class. */
    public static void doCommit(String message, String parent, TreeMap<String, String> tree) {
        // Generate commit object
        Commit commit = new Commit(message, parent, tree);
        String UID = commit.Hash();
        writeObject(join(OBJECT_DIR, UID), commit);

        // Update HEAD and Master
        writeContents(HEAD_DIR, UID);
        writeContents(join(BRANCH_DIR, readContentsAsString(join(BRANCH_DIR, "current"))), UID);
    }
    public static void init() {
        if (!GITLET_DIR.exists()) {
            if (!GITLET_DIR.mkdir()) {
                throw new java.lang.Error("Cannot create gitlet directory");
            }
            if (!OBJECT_DIR.mkdir()) {
                throw new java.lang.Error("Cannot create object directory");
            }
            if (!BRANCH_DIR.mkdir()) {
                throw new java.lang.Error("Cannot create branch directory");
            }
            writeContents(join(BRANCH_DIR, "current"), "master");
            Stage stage = new Stage();
            writeObject(TREE_DIR, stage);
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

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
        Stage stage;
        stage = readObject(TREE_DIR, Stage.class);
        stage.addAddition(filename, blobUID);

        // Retrieve previous commit
        String HEAD = readContentsAsString(HEAD_DIR);
        Commit previousCommit = readObject(join(OBJECT_DIR, HEAD), Commit.class);

        // Compare with contents in previous commit
        for (Map.Entry<String, String> entry : previousCommit.getTree().entrySet()) {
            String previousFilename = entry.getKey();
            String previousBlobUID = entry.getValue();

            if (Objects.equals(filename, previousFilename) &&
                    Objects.equals(stage.getAddition().get(filename), previousBlobUID)) {
                stage.removeAddition(previousFilename);
                break;
            }
        }

        // Store it in staging area
        writeObject(TREE_DIR, stage);
    }

    public static void commit(String message) {
        if (!TREE_DIR.exists()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // Retrieve current stage
        Stage currentStage = readObject(TREE_DIR, Stage.class);
        TreeMap<String, String> currentAddition = currentStage.getAddition();
        ArrayList<String> currentRemoval = currentStage.getRemoval();

        // Retrieve previous commit
        String HEAD = readContentsAsString(HEAD_DIR);
        Commit previousCommit = readObject(join(OBJECT_DIR, HEAD), Commit.class);
        TreeMap<String, String> previousTree = previousCommit.getTree();

        // Update current addition
        if (previousTree != null) {
            for (Map.Entry<String, String> entry : previousTree.entrySet()) {
                String filename = entry.getKey();
                String blobUID = entry.getValue();
                if (!previousTree.containsKey(filename)) {
                    currentAddition.put(filename, blobUID);
                }
            }
        }

        // Remove element in current addition if there is element in current removal
        if (currentRemoval != null) {
            for (String filename : currentRemoval) {
                currentAddition.remove(filename);
            }
        }

        // Create new commit
        doCommit(message, HEAD, currentAddition);

        // Initialize stage status
        currentStage.initialize();
        writeObject(TREE_DIR, currentStage);
    }

    public static void rm(String filename) {
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // Add filename into removal
        Stage stage = null;
        if (TREE_DIR.exists()) {
            stage = readObject(TREE_DIR, Stage.class);
            if (stage.exist(filename)) {
                // TODO: Unstage the file if it is currently staged for addition.
                stage.removeAddition(filename);
            } else {
                // Retrieve previous commit
                String HEAD = readContentsAsString(HEAD_DIR);
                Commit previousCommit = readObject(join(OBJECT_DIR, HEAD), Commit.class);
                TreeMap<String, String> previousTree = previousCommit.getTree();

                // TODO: If the file is tracked in the current commit, stage it for removal and remove the file from
                //  the working directory if the user has not already done so
                if (previousTree.containsKey(filename)) {
                    stage.addRemoval(filename);
                    deleteFile(file);
                }
            }
            // Store the stage status
            writeObject(TREE_DIR, stage);
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    public static void log() {
        // Retrieve previous commit
        String HEAD = readContentsAsString(HEAD_DIR);
        Commit commit;
        String date;
        String message;

        while (HEAD.length() > 0) {
            commit = readObject(join(OBJECT_DIR, HEAD), Commit.class);
            date = commit.getTimestamp();
            message = commit.getMessage();

            System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", HEAD, date, message);
            HEAD = commit.getParent();
        }
    }

    public static void globalLog() {
        List<String> fileList = plainFilenamesIn(OBJECT_DIR);
        Commit commit;
        String date;
        String message;
        for (String file : fileList) {
            try {
                commit = readObject(join(OBJECT_DIR, file), Commit.class);
                date = commit.getTimestamp();
                message = commit.getMessage();

                System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", file, date, message);
            } catch (Exception ignored) {}
        }
    }

    public static void find(String messageToFind) {
        List<String> fileList = plainFilenamesIn(OBJECT_DIR);
        Commit commit;
        String message;

        boolean flag = false;
        for (String file : fileList) {
            try {
                commit = readObject(join(OBJECT_DIR, file), Commit.class);
                message = commit.getMessage();

                if (message.contains(messageToFind)) {
                    System.out.println(file);
                    flag = true;
                }
            } catch (Exception ignored) {}
        }

        if (!flag) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        // Retrieve branches
        List<String> branches = plainFilenamesIn(BRANCH_DIR); // List is already sorted in plainFilenamesIn
        String currentBranch = readContentsAsString(join(BRANCH_DIR, "current"));

        // Retrieve current stage
        Stage currentStage = readObject(TREE_DIR, Stage.class);
        TreeMap<String, String> currentAddition = currentStage.getAddition(); // TreeMap is already sorted
        ArrayList<String> currentRemoval = currentStage.getRemoval();
        HashSet<String> currentRemovalSet = new HashSet<>(currentRemoval);
        Collections.sort(currentRemoval);

        // Retrieve the latest commit
        String HEAD = readContentsAsString(HEAD_DIR);
        Commit currentCommit = readObject(join(OBJECT_DIR, HEAD), Commit.class);
        TreeMap<String, String> currentTree = currentCommit.getTree();

        // Retrieve all the file name in the directory
        List<String> files = plainFilenamesIn(CWD);
        Set<String> filesSet = new HashSet<>(files);

        // List branches
        System.out.println("=== Branches ===");
        for (String branch : branches) {
            if (Objects.equals(branch, "current")) continue;
            if (Objects.equals(branch, currentBranch)) {
                branch = "*" + branch;
            }
            System.out.println(branch);
        }
        System.out.println();

        // List staged files
        System.out.println("=== Staged Files ===");
        for (Map.Entry<String, String> entry : currentAddition.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();

        // List removed files
        System.out.println("=== Removed Files ===");
        for (String filename : currentRemoval) {
            System.out.println(filename);
        }
        System.out.println();

        // List Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (Map.Entry<String, String> entry : currentTree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            if (filesSet.contains(filename)) {
                Blob blob = new Blob(join(join(CWD, filename)));
                String fileBlobUID = blob.Hash();
                if ((!Objects.equals(fileBlobUID, blobUID))) {
                    if (!currentAddition.containsKey(filename)) {
                        // TODO: Tracked in the current commit, changed in the working directory, but not staged
                        System.out.println(filename + " (modified)1");
                    } else {
                        // TODO: Staged for addition, but with different contents than in the working directory
                        String stageBlobUID = currentAddition.get(filename);
                        if (!Objects.equals(fileBlobUID, stageBlobUID)) {
                            System.out.println(filename + " (modified)2");
                        }
                    }
                }
            } else {
                // TODO: Not staged for removal, but tracked in the current commit and deleted from the working directory
                if (!currentRemovalSet.contains(filename)) {
                    System.out.println(filename + " (deleted)");
                }
            }
        }
        // TODO: Staged for addition, but deleted in the working directory
        for (Map.Entry<String, String> entry : currentAddition.entrySet()) {
            String filename = entry.getKey();
            if (!filesSet.contains(filename)) {
                System.out.println(filename + " (modified)3");
            }
        }
        System.out.println();

        // List untracked file
        // TODO: files present in the working directory but neither staged for addition nor tracked
        System.out.println("=== Untracked Files ===");
        for (String filename : filesSet) {
            if (filesToBeIgnored.contains(filename)) continue;
            if (!currentTree.containsKey(filename) && !currentAddition.containsKey(filename)) {
                System.out.println(filename);
            }
        }
        System.out.println();
        System.out.println(currentAddition);
        System.out.println(currentRemoval);
    }
}
