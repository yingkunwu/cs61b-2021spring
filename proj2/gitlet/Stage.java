package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import static gitlet.Utils.join;

public class Stage implements Serializable {

    private TreeMap<String, String> addition;
    private ArrayList<String> removal;

    public Stage() {
        addition = new TreeMap<>();
        removal = new ArrayList<>();
    }

    public void initialize() {
        addition = new TreeMap<>();
        removal = new ArrayList<>();
    }

    public TreeMap<String, String> getAddition() {
        return this.addition;
    }

    public ArrayList<String> getRemoval() {
        return this.removal;
    }

    public void addAddition(String filename, String blobUID) {
        // if blob with same filename exists, delete it
        if (exist(filename)) {
            String UID = addition.get(filename);
            Utils.deleteFile(join(Repository.OBJECT_DIR, UID));
        }
        addition.put(filename, blobUID);
    }

    public void addRemoval(String filename) {
        addition.remove(filename);
        removal.add(filename);
    }

    public void removeAddition(String filename) {
        if (!exist(filename)) {
            System.out.println("Unable to remove");
            System.exit(0);
        }
        String UID = addition.get(filename);
        Utils.deleteFile(join(Repository.OBJECT_DIR, UID));
        addition.remove(filename);
    }

    public boolean exist(String filename) {
        return addition.containsKey(filename);
    }
}
