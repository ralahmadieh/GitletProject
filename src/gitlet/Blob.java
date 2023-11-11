package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Blob implements Serializable {

    public File cwdfile;
    public String fileName;
    public String shaName;
    public byte[] content;

    public Blob(String name) {
        fileName = name;
        cwdfile = new File(fileName); //should lead to the file in cwd?
        if (cwdfile.exists()) {
            content = Utils.readContents(cwdfile); //Return the entire contents of FILE as a byte array
            shaName = Utils.sha1(content, fileName);
        }
    }

    public void saveBlob() throws IOException {
        File f = Utils.join(Main.BLOBS_FOLDER, this.shaName);
        f.createNewFile();
        Utils.writeObject(f, this);
    }


}