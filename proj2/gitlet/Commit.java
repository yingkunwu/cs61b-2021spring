package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
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
    private String parent;
    private String timestamp;
    private TreeMap<String, String> tree;

    /* TODO: fill in the rest of this class. */
    public Commit(String message, String parent, TreeMap<String, String> tree) {
        this.message = message;
        this.parent = parent;
        this.tree = tree;

        if (this.parent.length() == 0) {
            this.timestamp = new Date(0).toString();
        } else {
            this.timestamp = new Date().toString();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getParent() {
        return this.parent;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public TreeMap<String, String> getTree() {
        return this.tree;
    }

    public String Hash() {
        List<Object> list = new ArrayList<>();
        list.add(this.message);
        list.add(this.parent);
        list.add(this.timestamp);

        for(Map.Entry<String, String> entry : tree.entrySet()) {
            String filename = entry.getKey();
            String blobUID = entry.getValue();
            list.add(filename);
            list.add(blobUID);
        }

        return Utils.sha1(list);
    }
}
