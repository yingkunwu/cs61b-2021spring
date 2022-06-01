package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.SimpleDateFormat;
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
    private String parent1;
    private String parent2;
    private String timestamp;
    private TreeMap<String, String> tree;

    /* TODO: fill in the rest of this class. */
    public Commit(String message, String parent1, String parent2, TreeMap<String, String> tree) {
        this.message = message;
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.tree = tree;

        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy");

        if (this.parent1.length() == 0) {
            this.timestamp = formatter.format(new Date(0)) + " +0800";
        } else {
            this.timestamp = formatter.format(new Date()) + " +0800";
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getParent() {
        return this.parent1;
    }

    public String getSecondParent() {
        return this.parent2;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public TreeMap<String, String> getTree() {
        return this.tree;
    }

    public boolean isMerge() {
        return this.parent2.length() > 0;
    }

    public String Hash() {
        List<Object> list = new ArrayList<>();
        list.add(this.message);
        list.add(this.parent1);
        list.add(this.parent2);
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
