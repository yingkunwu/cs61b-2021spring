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
    public static final File MASTER_DIR = join(GITLET_DIR, "MASTER");
    public static final File TREE_DIR = join(GITLET_DIR, "TREE");

    /* TODO: fill in the rest of this class. */
    public static void doCommit(String message, String parent, TreeMap<String, String> tree) {
        // Generate commit object
        Commit commit = new Commit(message, parent, tree);
        String UID = commit.Hash();
        Utils.writeObject(join(OBJECT_DIR, UID), commit);

        // Update HEAD and Master
        Utils.writeContents(HEAD_DIR, UID);
        Utils.writeContents(MASTER_DIR, UID);
    }
    public static void init() {
        if (!GITLET_DIR.exists()) {
            if (!GITLET_DIR.mkdir()) {
                throw new java.lang.Error("Cannot create gitlet directory");
            }
            if (!OBJECT_DIR.mkdir()) {
                throw new java.lang.Error("Cannot create object directory");
            }
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
        Utils.writeObject(join(Repository.OBJECT_DIR, blobUID), blob);

        // Retrieve previous staging status or generate a new one if not exist
        Stage stage;
        if (TREE_DIR.exists()) {
            stage = Utils.readObject(TREE_DIR, Stage.class);
        } else {
            stage = new Stage();
        }
        stage.addAddition(filename, blobUID);

        // Retrieve previous commit
        String HEAD = Utils.readContentsAsString(HEAD_DIR);
        Commit previousCommit = Utils.readObject(join(OBJECT_DIR, HEAD), Commit.class);

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
        Utils.writeObject(TREE_DIR, stage);
    }

    public static void commit(String message) {
        if (!TREE_DIR.exists()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // Retrieve current stage
        String HEAD = Utils.readContentsAsString(HEAD_DIR);
        Stage currentStage = Utils.readObject(TREE_DIR, Stage.class);
        TreeMap<String, String> currentAddition = currentStage.getAddition();
        ArrayList<String> currentRemoval = currentStage.getRemoval();

        // Retrieve previous commit
        Commit previousCommit = Utils.readObject(join(OBJECT_DIR, HEAD), Commit.class);
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

        // Clean Stage file
        Utils.deleteFile(TREE_DIR);
    }

    public static void rm(String filename) {
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // Delete the file
        Utils.deleteFile(file);

        // Add filename into removal
        Stage stage = null;
        if (TREE_DIR.exists()) {
            stage = Utils.readObject(TREE_DIR, Stage.class);
            if (stage.exist(filename)) {
                stage.removeAddition(filename);
            } else {
                // Retrieve previous commit
                String HEAD = Utils.readContentsAsString(HEAD_DIR);
                Commit previousCommit = Utils.readObject(join(OBJECT_DIR, HEAD), Commit.class);

                // Compare with contents in previous commit
                for (Map.Entry<String, String> entry : previousCommit.getTree().entrySet()) {
                    String previousFilename = entry.getKey();
                    String previousBlobUID = entry.getValue();

                    if (Objects.equals(filename, previousFilename) &&
                            Objects.equals(stage.getAddition().get(filename), previousBlobUID)) {
                        stage.addRemoval(filename);
                        break;
                    }
                }
            }
            // Store it in staging area
            Utils.writeObject(TREE_DIR, stage);
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    public static void log() {
        // Retrieve previous commit
        String HEAD = Utils.readContentsAsString(HEAD_DIR);
        Commit commit;
        String date;
        String message;

        while (HEAD.length() > 0) {
            commit = Utils.readObject(join(OBJECT_DIR, HEAD), Commit.class);
            date = commit.getTimestamp();
            message = commit.getMessage();
            HEAD = commit.getParent();

            System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", HEAD, date, message);
        }
    }

    public static void globalLog() {
        List<String> fileList = Utils.plainFilenamesIn(OBJECT_DIR);
        Commit commit;
        String date;
        String message;
        for (String file : fileList) {
            try {
                commit = Utils.readObject(join(OBJECT_DIR, file), Commit.class);
                date = commit.getTimestamp();
                message = commit.getMessage();

                System.out.printf("===\ncommit %s\nDate: %s\n%s\n\n", file, date, message);
            } catch (Exception ignored) {}
        }
    }

    public static void find(String messageToFind) {
        List<String> fileList = Utils.plainFilenamesIn(OBJECT_DIR);
        Commit commit;
        String message;

        boolean flag = false;
        for (String file : fileList) {
            try {
                commit = Utils.readObject(join(OBJECT_DIR, file), Commit.class);
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
}
