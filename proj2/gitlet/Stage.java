package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import static gitlet.Utils.join;

public class Stage implements Serializable {

    private final TreeMap<String, String> addition;
    private final ArrayList<String> removal;

    public Stage() {
        addition = new TreeMap<>();
        removal = new ArrayList<>();
    }

    public void initialize() {
        addition.clear();
        removal.clear();
    }

    public TreeMap<String, String> getAddition() {
        return this.addition;
    }

    public ArrayList<String> getRemoval() {
        return this.removal;
    }

    public boolean exist(String filename) {
        return addition.containsKey(filename);
    }

    public boolean empty() {
        return addition.isEmpty() && removal.isEmpty();
    }

    public void addAddition(String filename, String blobUID) {
        removeRemoval(filename);
        addition.put(filename, blobUID);
    }

    public void removeAddition(String filename) {
        addition.remove(filename);
    }

    public void addRemoval(String filename) {
        removeAddition(filename);
        removal.add(filename);
    }

    public void removeRemoval(String filename) {
        removal.remove(filename);
    }
}
