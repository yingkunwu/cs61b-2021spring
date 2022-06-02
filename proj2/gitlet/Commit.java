package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet commit object.
 *  This object store all the information in Commit.
 *  The tree structure serve as the snapshot of the current tracked file when commit.
 *  @author Ethan
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private ArrayList<String> parent;
    private String timestamp;
    private TreeMap<String, String> tree;

    /* TODO: fill in the rest of this class. */
    public Commit(String message, ArrayList<String> parent, TreeMap<String, String> tree) {
        this.message = message;
        this.parent = parent;
        this.tree = tree;

        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy");

        if (this.parent.get(0).length() == 0) {
            this.timestamp = formatter.format(new Date(0)) + " +0800";
        } else {
            this.timestamp = formatter.format(new Date()) + " +0800";
        }
    }

    public String getMessage() {
        return this.message;
    }

    public ArrayList<String> getParent() {
        return parent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public TreeMap<String, String> getTree() {
        return tree;
    }

    public boolean isMerge() {
        return parent.size() > 1;
    }

    public String Hash() {
        List<Object> list = new ArrayList<>();
        list.add(message);
        list.add(timestamp);
        list.addAll(parent);

        for (Map.Entry<String, String> entry : tree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            list.add(filename);
            list.add(blobUID);
        }

        return Utils.sha1(list);
    }
}
