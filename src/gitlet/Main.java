package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Helen Hoang, Rawan Ahmadieh
 */
public class Main {

    /** Main metadata folder. */
    static final File GITLET_FOLDER = new File(".gitlet");
    static final File STAGING_FOLDER = Utils.join(GITLET_FOLDER, ".staging");
    static final File ADD_STAGE = Utils.join(GITLET_FOLDER, ".add");
    static final File RM_STAGE = Utils.join(GITLET_FOLDER, ".rm");
    static final File COMMITS_FOLDER = Utils.join(GITLET_FOLDER, ".commits");
    static final File BLOBS_FOLDER = Utils.join(GITLET_FOLDER, ".blobs");

    static File HEAD = Utils.join(GITLET_FOLDER, ".head"); //shaName of the head commit
    static File MASTER = Utils.join(GITLET_FOLDER, ".master"); //holds head of the master branch

    static final File ALL_COMMITS = Utils.join(GITLET_FOLDER, ".allcommits");
    static final File ALL_BRANCHES = Utils.join(GITLET_FOLDER, ".branches");
    static final File CURR_BRANCH = Utils.join(GITLET_FOLDER, ".branch");




    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (!GITLET_FOLDER.exists() && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        switch (args[0]) {
            case "init":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitinit();
                break;
            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitadd(args[1]);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                } else if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitcommit(args[1]);
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitrm(args[1]);
                break;
            case "log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitlog();
                break;
            case "global-log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitgloballog();
                break;
            case "find":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitfind(args[1]);
                break;
            case "status":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitstatus();
                break;
            case "checkout":
                gitcheckout(args);
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitbranch(args[1]);
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                rmBranch(args[1]);
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitreset(args[1]);
                break;
            case "merge":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                gitmerge(args[1]);
                break;
            case "rebase":
                System.out.println("Rebase not implemented.");
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    public static void gitinit() throws IOException {
        //Creates a new Gitlet version-control system in the current directory.
        //This system will automatically start with one empty commit
        if (GITLET_FOLDER.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_FOLDER.mkdir();
            STAGING_FOLDER.mkdir();
            COMMITS_FOLDER.mkdir();
            BLOBS_FOLDER.mkdir();
            Commit init = new Commit();
            HEAD.createNewFile();
            Utils.writeContents(HEAD, init.shaName); //saving shaname in head file
            MASTER.createNewFile();
            Utils.writeContents(MASTER, init.shaName); //saving shaname in master head branch
            init.saveCommit();
            ADD_STAGE.createNewFile(); //file will contain array of blob shanames
            RM_STAGE.createNewFile();
            HashMap addstage = new HashMap();
            HashMap rmstage = new HashMap();
            Utils.writeObject(ADD_STAGE, addstage);
            Utils.writeObject(RM_STAGE, rmstage);
            ALL_COMMITS.createNewFile();
            HashMap allcommits = new HashMap();
            allcommits.put(init.shaName, init.message);
            Utils.writeObject(ALL_COMMITS, allcommits);
            ALL_BRANCHES.createNewFile();
            HashMap allbranches = new HashMap();
            allbranches.put(init.branch, init.shaName);
            Utils.writeObject(ALL_BRANCHES, allbranches);
            CURR_BRANCH.createNewFile();
            Utils.writeContents(CURR_BRANCH, init.branch);
        }
    }

    public static void gitadd(String fileName) throws IOException {
        //Stages the file for addition
        Blob adding = new Blob(fileName);
        if (!adding.cwdfile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        File f = Utils.join(STAGING_FOLDER, adding.shaName); //points to the file inside the staging directory
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        HashMap rmstage = Utils.readObject(RM_STAGE, HashMap.class);
        Commit head = fromShaC(Utils.readContentsAsString(HEAD));

        if (rmstage.containsKey(fileName)) {
            rmstage.remove(fileName); //if staged for removal, sike
            Utils.writeObject(RM_STAGE, rmstage);
            return;
        }

        if (head.blobs.containsValue(adding.shaName)) { //if file is already tracked w/ same content
            return;
        }

        if (addstage.containsKey(fileName)) {
            if (addstage.containsValue(adding.shaName)) { //if already added identical file w/ same content
                return;
            } else {
                File j = Utils.join(STAGING_FOLDER, (String) addstage.get(fileName));
                j.delete(); //deletes if already staged for addition, rewrites w new content later
                addstage.remove(fileName); //adds back later, don't want doubles
            }
        }
        f.createNewFile(); //creates empty file named after shaName in staging directory
        Utils.writeObject(f, adding); //adds contents of the file thru blob
        addstage.put(fileName, adding.shaName); //file listed to be added
        Utils.writeObject(ADD_STAGE, addstage);
    }

    public static void gitcommit(String message) throws IOException {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Commit parent = fromShaC(Utils.readContentsAsString(HEAD)); //current head is parent commit
        String branch = Utils.readContentsAsString(CURR_BRANCH);
        Commit current = new Commit(message, parent.shaName, branch);
        HashMap allcommits = Utils.readObject(ALL_COMMITS, HashMap.class);
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        HashMap rmstage = Utils.readObject(RM_STAGE, HashMap.class);
        if (addstage.isEmpty()) {
            if (rmstage.isEmpty()) {
                System.out.println("No changes added to the commit.");
                System.exit(0);
            }
        }

        current.blobs.putAll(addstage);//adding blobs from staging area to current commit

        for (Object s : rmstage.keySet()) {
            current.blobs.remove(s); //removing blobs from current commit
        }

        Utils.writeContents(HEAD, current.shaName); //changing head
        current.saveCommit(); //saving commit

        for (Object s : current.blobs.keySet()) {
            String blobsha = current.blobs.get(s); //gets shaName from blob
            File f = Utils.join(STAGING_FOLDER, blobsha); //file in staging directory
            if (f.exists()) {
                Blob blobby = fromShaBStage(blobsha); //reconstructs the staged blob from shaName
                blobby.saveBlob(); //saves blobs to blobs_folder after making changes
                f.delete(); //removes file from staging directory
            }
        }

        allcommits.put(current.shaName, current.message);
        Utils.writeObject(ALL_COMMITS, allcommits);
        Utils.writeObject(ADD_STAGE, new HashMap()); //clears add/remove stage files
        Utils.writeObject(RM_STAGE, new HashMap());

        //need to change current branch's head every time u commit
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        allbranches.replace(current.branch, current.shaName);
        Utils.writeObject(ALL_BRANCHES, allbranches);
    }

    //reconstructs Commit from its shaName
    public static Commit fromShaC(String sha) {
        File f = Utils.join(COMMITS_FOLDER, sha);
        return Utils.readObject(f, Commit.class);
    }

    //reconstructs tracked Blob from its shaName
    public static Blob fromShaB(String sha) {
        File f = Utils.join(BLOBS_FOLDER, sha);
        return Utils.readObject(f, Blob.class);
    }

    //reconstructs staged Blob from its shaName
    public static Blob fromShaBStage(String sha) {
        File f = Utils.join(STAGING_FOLDER, sha);
        return Utils.readObject(f, Blob.class);
    }

    public static void gitrm(String fileName) {
        Blob removing = new Blob(fileName);
        Commit head = fromShaC(Utils.readContentsAsString(HEAD));
        File f = new File(fileName); //points to file in cwd
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        HashMap rmstage = Utils.readObject(RM_STAGE, HashMap.class);

        if (head.blobs.containsKey(fileName) && !f.exists()) { //if manually deleted but tracked in commit
            rmstage.put(fileName, head.blobs.get(fileName)); //stage for removal
            Utils.writeObject(RM_STAGE, rmstage);
            System.exit(0);
        }

        File g = Utils.join(STAGING_FOLDER, removing.shaName); //if the file is staged
        if (!g.exists() && !head.blobs.containsValue(removing.shaName)) {
            System.out.println("No reason to remove the file.");
        }

        if (addstage.containsKey(fileName)) {
            g.delete(); //if file is staged for addition, unstage it
            addstage.remove(fileName);
        }

        if (head.blobs.containsValue(removing.shaName)) { //if file is tracked in commit,
            rmstage.put(fileName, removing.shaName);//stage for removal
            f.delete(); //remove from cwd
        }

        Utils.writeObject(RM_STAGE, rmstage); //alters staging files
        Utils.writeObject(ADD_STAGE, addstage);
    }

    public static void gitlog() {
        String head = Utils.readContentsAsString(HEAD);
        Commit toPrint;

        for (String p = head; p != null; ) {
            toPrint = fromShaC(p);
            System.out.println("===");
            System.out.println("commit " + toPrint.shaName);
            System.out.println("Date: " + toPrint.timestamp);
            System.out.println(toPrint.message);
            System.out.println();
            p = toPrint.parent;
        }
    }

    public static void gitbranch(String branch) throws IOException {
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        if (allbranches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        allbranches.put(branch, Utils.readContentsAsString(HEAD));
        Utils.writeObject(ALL_BRANCHES, allbranches);
    }

    public static String shortID(String id) {
        HashMap<String, String> allcommits = Utils.readObject(ALL_COMMITS, HashMap.class);
        for (String key : allcommits.keySet()) {
            if (key.contains(id)) {
                return key;
            }
        }
        return null;
    }

    public static void gitcheckout(String... args) {
        Commit head = fromShaC(Utils.readContentsAsString(HEAD));
        if (args.length == 3 && args[1].equals("--")) {
            checkoutHelper(args[2], head);
        } else if (args.length == 4 && args[2].equals("--")) {
            String id = args[1];
            if (args[1].length() < 40) {
                id = shortID(args[1]);
            }
            File f = Utils.join(COMMITS_FOLDER, id);
            if (!f.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            Commit chosenOne = fromShaC(id);
            checkoutHelper(args[3], chosenOne);
        } else if (args.length == 2) {
            HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
            String currentbr = Utils.readContentsAsString(CURR_BRANCH);
            String brname = args[1];
            if (!allbranches.containsKey(brname)) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            if (currentbr.equals(brname)) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            Commit brhead = fromShaC((String) allbranches.get(brname));
            if (untrackedFiles()) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            Utils.writeContents(HEAD, brhead.shaName); //new head commit is branchHead
            Utils.writeContents(CURR_BRANCH, brname);
            Set<String> fn = brhead.blobs.keySet(); //set of fileNames in branch head commit
            Set<String> og = head.blobs.keySet(); //set of fileNames in original head commit
            for (String o : og) {
                if (!fn.contains(o)) { //if file in original head is not present in branch head
                    File deleting = new File(o);
                    deleting.delete(); //delete file from cwd
                }
            }
            for (String f : fn) {
                checkoutHelper(f, brhead); //checks out all files in branch head commit
            }
            Utils.writeObject(ADD_STAGE, new HashMap()); //clears staging area
            Utils.writeObject(RM_STAGE, new HashMap());
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static void checkoutHelper(String file, Commit search) {
        //rewrites file in cwd to whatever fileName is passed in
        if (search.blobs.containsKey(file)) {
            Blob b = fromShaB(search.blobs.get(file));
            File replacing = new File(file);
            Utils.writeContents(replacing, b.content);
        } else {
            System.out.print("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public static void gitgloballog() {
        HashMap allcommits = Utils.readObject(ALL_COMMITS, HashMap.class);
        Set<String> keys = allcommits.keySet();
        Commit toPrint;
        for (String key : keys) {
            toPrint = fromShaC(key);
            System.out.println("===");
            System.out.println("commit " + toPrint.shaName);
            System.out.println("Date: " + toPrint.timestamp);
            System.out.println(toPrint.message);
            System.out.println();
        }
    }

    public static void gitfind(String message) {
        HashMap allcommits = Utils.readObject(ALL_COMMITS, HashMap.class);
        if (!allcommits.containsValue(message)) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        for(Object entry: allcommits.keySet()) {
            if (allcommits.get(entry).equals(message)) {
                System.out.println(entry);
            }
        }
    }

    public static void rmBranch(String name) {
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        if (!allbranches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String headBranch = Utils.readContentsAsString(CURR_BRANCH);
        if (name.equals(headBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        else {
            allbranches.remove(name);
            Utils.writeObject(ALL_BRANCHES, allbranches);
        }
    }

    public static void gitstatus() {
        System.out.println("=== Branches ===");
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        String headBranch = Utils.readContentsAsString(CURR_BRANCH);
        ArrayList keys = new ArrayList(allbranches.keySet());
        Collections.sort(keys);
        for (Object branch : keys) {
            if (branch.equals(headBranch)) {
                System.out.print("*");
            }
            System.out.print(branch);
            System.out.println();
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        for (Object name : addstage.keySet()) {
            System.out.println(name);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        HashMap rmstage = Utils.readObject(RM_STAGE, HashMap.class);
        for (Object name : rmstage.keySet()) {
            System.out.println(name);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void gitreset(String commID) {
        String id = commID;
        if (commID.length() < 40) {
            id = shortID(commID);
        }
        File f = Utils.join(COMMITS_FOLDER, id);
        if (!f.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        if (untrackedFiles()) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        Commit given = fromShaC(id);
        for (Object fileName : given.blobs.keySet()) {
            checkoutHelper((String) fileName, given);
        }
        Commit head = fromShaC(Utils.readContentsAsString(HEAD));
        Set<String> fn = given.blobs.keySet(); //set of fileNames in branch head commit
        Set<String> og = head.blobs.keySet(); //set of fileNames in original head commit
        for (String o : og) {
            if (!fn.contains(o)) { //if file in original head is not present in branch head
                File deleting = new File(o);
                deleting.delete(); //delete file from cwd
            }
        }
        Utils.writeContents(HEAD, id); //head changed to given commit
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        allbranches.replace(Utils.readContentsAsString(CURR_BRANCH), id); //given commit becomes head of current branch
        Utils.writeObject(ALL_BRANCHES, allbranches);
        Utils.writeObject(ADD_STAGE, new HashMap<>()); //staging area cleared
        Utils.writeObject(RM_STAGE, new HashMap<>());
    }

    public static boolean untrackedFiles() {
        Commit head = fromShaC(Utils.readContentsAsString(HEAD));
        List<String> allfiles = Utils.plainFilenamesIn();
        List<String> txtfiles = new ArrayList<>();
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        for (String file : allfiles) {
            if (file.contains(".txt")) {
                txtfiles.add(file);
            }
        }
        for (String file : txtfiles) {
            if (!head.blobs.containsKey(file) && !addstage.containsKey(file)) {
                return true;
            }
        }
        return false;
    }

    public static void gitmerge(String givenbr) throws IOException {
        Commit currhead = fromShaC(Utils.readContentsAsString(HEAD));
        HashMap allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        if (untrackedFiles()) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        if (!allbranches.containsKey(givenbr)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (currhead.branch == givenbr) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit givenhead = fromShaC((String) allbranches.get(givenbr));
        HashMap addstage = Utils.readObject(ADD_STAGE, HashMap.class);
        HashMap rmstage = Utils.readObject(RM_STAGE, HashMap.class);
        if (!addstage.isEmpty() || !rmstage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        String splitSha = splitFinder(givenbr);
        Commit splitpoint = fromShaC(splitSha);
        if (splitSha.equals(currhead.shaName)) {
            gitcheckout("checkout", givenbr);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        if (splitSha.equals(givenhead.shaName)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        boolean conflicts = false;
        for (String file : givenhead.blobs.keySet()) {
            if (ifGivenModified(splitpoint, currhead, givenhead, file)) {
                gitcheckout(givenhead.shaName, "--", file);
                addstage.put(file, givenhead.blobs.get(file));
                continue;
            }
            if (ifOnlyGiven(splitpoint, currhead, givenhead, file)) {
                gitcheckout(givenhead.shaName, "--", file);
                addstage.put(file, givenhead.blobs.get(file));
                continue;
            }
            if (goneFromGiven(splitpoint, currhead, givenhead, file)) {
                File removing = new File(file);
                rmstage.put(file, currhead.blobs.get(file));
                removing.delete();
                continue;
            }
            if (inConflictOne(splitpoint, currhead, givenhead, file)) {
                Blob currb = fromShaB(currhead.blobs.get(file));
                Blob givb = fromShaB(givenhead.blobs.get(file));
                File cwdf = new File(file);
                Utils.writeContents(cwdf, "<<<<<<< HEAD" + currb.content
                        + "=======" + givb.content + ">>>>>>>");
                conflicts = true;
                Blob changed = new Blob(file);
                addstage.put(file, changed.shaName);
                continue;
            }
            if (inConflictTwo(splitpoint, currhead, givenhead, file)) {
                Blob currb = fromShaB(currhead.blobs.get(file));
                File cwdf = new File(file);
                Utils.writeContents(cwdf, "<<<<<<< HEAD" + currb.content
                        + "=======" + System.lineSeparator() + ">>>>>>>");
                conflicts = true;
                Blob changed = new Blob(file);
                addstage.put(file, changed.shaName);
                continue;
            }
            if (inConflictThree(splitpoint, currhead, givenhead, file)) {
                Blob givb = fromShaB(givenhead.blobs.get(file));
                File cwdf = new File(file);
                Utils.writeContents(cwdf, "<<<<<<< HEAD" + System.lineSeparator()
                        + "=======" + givb.content + ">>>>>>>");
                conflicts = true;
                Blob changed = new Blob(file);
                addstage.put(file, changed.shaName);
                continue;
            }
            if (inConflictFour(splitpoint, currhead, givenhead, file)) {
                Blob currb = fromShaB(currhead.blobs.get(file));
                Blob givb = fromShaB(givenhead.blobs.get(file));
                File cwdf = new File(file);
                Utils.writeContents(cwdf, "<<<<<<< HEAD" + currb.content
                        + "=======" + givb.content + ">>>>>>>");
                conflicts = true;
                Blob changed = new Blob(file);
                addstage.put(file, changed.shaName);
                continue;
            }
        }
        gitcommit("Merged" + givenbr + "into" + currhead.branch + ".");
        Commit merged = fromShaC(Utils.readContentsAsString(HEAD));
        merged.parent2 = givenhead.shaName;
        if (conflicts) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static boolean ifGivenModified(Commit splitpoint, Commit current, Commit given, String file) {
        //Any files that have been modified in the given branch since the split point,
        // but not modified in the current branch since the split point should be changed
        // to their versions in the given branch (checked out from the commit at the front
        // of the given branch). These files should then all be automatically staged.
        boolean mod = false;
        if (splitpoint.blobs.keySet().contains(file) && current.blobs.keySet().contains(file)
                && given.blobs.keySet().contains(file)) {
            if (splitpoint.blobs.get(file).equals(current.blobs.get(file))
                    && !splitpoint.blobs.get(file).equals(given.blobs.get(file))) {
                mod = true;
            }
        }
        return mod;
    }

    public static boolean goneFromGiven(Commit splitpoint, Commit current, Commit given, String file) {
        //Any files present at the split point, unmodified in the current branch,
        // and absent in the given branch should be removed (and untracked).
        boolean gone = false;
        if (splitpoint.blobs.keySet().contains(file) && current.blobs.keySet().contains(file)
                && !given.blobs.keySet().contains(file)) {
            if (splitpoint.blobs.get(file).equals(current.blobs.get(file))) {
                gone = true;
            }
        }
        return gone;
    }

    public static boolean ifOnlyGiven(Commit splitpoint, Commit current, Commit given, String file) {
        //Any files that were not present at the split point and are present only in the given
        // branch should be checked out and staged.
        boolean only = false;
        if (!splitpoint.blobs.keySet().contains(file)) {
            if (!current.blobs.keySet().contains(file)) {
                if (given.blobs.keySet().contains(file)) {
                    only = true;
                }
            }
        }
        return only;
    }
    public static boolean inConflictOne(Commit splitpoint, Commit current, Commit given, String file) {
        //the contents of both are changed and different from each other
        boolean conf1 = false;
        if (splitpoint.blobs.keySet().contains(file) && current.blobs.keySet().contains(file)
                && given.blobs.keySet().contains(file)) {
            if (!splitpoint.blobs.get(file).equals(current.blobs.get(file))
                    && !splitpoint.blobs.get(file).equals(given.blobs.get(file))) {
                if (!current.blobs.get(file).equals(given.blobs.get(file))) {
                    conf1 = true;
                }
            }
        }
        return conf1;
    }

    public static boolean inConflictTwo(Commit splitpoint, Commit current, Commit given, String file) {
        //the contents of one are changed and the other file is delete
        // deleted from given
        boolean conf2 = false;
        if (splitpoint.blobs.keySet().contains(file) && current.blobs.keySet().contains(file)
                && !given.blobs.keySet().contains(file)) {
            if (!splitpoint.blobs.get(file).equals(current.blobs.get(file))) {
                conf2 = true;
            }
        }
        return conf2;
    }

    public static boolean inConflictThree(Commit splitpoint, Commit current, Commit given, String file) {
        //the contents of one are changed and the other file is delete
        // deleted from current
        boolean conf3 = false;
        if (splitpoint.blobs.keySet().contains(file) && !current.blobs.keySet().contains(file)
                && given.blobs.keySet().contains(file)) {
            if (!splitpoint.blobs.get(file).equals(given.blobs.get(file))) {
                conf3 = true;
            }
        }
        return conf3;
    }

    public static boolean inConflictFour(Commit splitpoint, Commit current, Commit given, String file) {
        //the file was absent at the split point and has different contents in the given and current branches
        boolean conf4 = false;
        if (!splitpoint.blobs.keySet().contains(file) && current.blobs.keySet().contains(file)
                && given.blobs.keySet().contains(file)) {
            if (!current.blobs.get(file).equals(given.blobs.get(file))) {
                conf4 = true;
            }
        }
        return conf4;
    }

    public static String splitFinder(String branch) {
        HashMap<String, String> allbranches = Utils.readObject(ALL_BRANCHES, HashMap.class);
        ArrayList<String> check = new ArrayList<>();
        for (String given = allbranches.get(branch); given != null; ) {
            Commit c = fromShaC(given);
            check.add(given);
            given = c.parent;
        }
        Commit currhead = fromShaC(Utils.readContentsAsString(HEAD));
        for (String splitSha = currhead.shaName; splitSha != null; ) {
            Commit c = fromShaC(splitSha);
            if (check.contains(splitSha)) {
                return c.shaName;
            }
            splitSha = c.parent;
        }
        return null;
    }
}