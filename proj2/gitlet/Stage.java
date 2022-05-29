package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import static gitlet.Utils.join;

public class Stage implements Serializable {

    private final TreeMap<String, String> addition;
    private final ArrayList<String> removal;

    public Stage() {
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
        if (addition.containsKey(filename)) {
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
        String UID = addition.get(filename);
        Utils.deleteFile(join(Repository.OBJECT_DIR, UID));
        addition.remove(filename);
    }
}
