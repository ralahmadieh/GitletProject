package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {

    public String parent;
    public String parent2;
    public String shaName;
    public String timestamp;
    public String message;
    public String branch;
    public File file;
    public HashMap<String, String> blobs = new HashMap<>(); //key fileName, value sha1



    private String makeTimestamp() {
        Date d = new Date();
        SimpleDateFormat t = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        return t.format(d);
    }

    //constructor for initial commit owo
    public Commit() {
        message = "initial commit";
        parent = null;
        branch = "master";
        Date d = new Date(0);
        SimpleDateFormat t = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        timestamp = t.format(d);
        shaName = Utils.sha1(message, timestamp);
        file = Utils.join(Main.COMMITS_FOLDER, shaName);
    }

    //constructor for owo-ing
    public Commit(String msg, String p, String br) {
        message = msg;
        parent = p; //shaName of parent
        branch = br;
        timestamp = makeTimestamp();
        shaName = Utils.sha1(message, timestamp);
        file = Utils.join(Main.COMMITS_FOLDER, shaName);
        Commit parent = Main.fromShaC(p); //reconstructing parent commit
        this.blobs.putAll(parent.blobs); //current blobs = previous blobs
        //inherits blobs from parent commit
    }

    public void saveCommit() throws IOException {
        this.file.createNewFile();
        Utils.writeObject(file, this);
    }

}