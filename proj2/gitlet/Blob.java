package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    private final String content;

    public Blob(File file) {
        this.content = Utils.readContentsAsString(file);
    }

    public Blob(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public String Hash() {
        return Utils.sha1(this.content);
    }
}
