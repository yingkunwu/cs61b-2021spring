package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        String firstArg = null;

        try {
            firstArg = args[0];
        } catch (java.lang.ArrayIndexOutOfBoundsException exception) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        // TODO: If a user inputs a command with the wrong number or format of operands,
        //  print the message Incorrect operands. and exit.

        // TODO: If a user inputs a command that requires being in an initialized Gitlet working directory
        //  (i.e., one containing a .gitlet subdirectory), but is not in such a directory,
        //  print the message Not in an initialized Gitlet directory.

        switch(firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                if (args.length < 2) {
                    System.out.println("Please enter a filename.");
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "rm":
                if (args.length < 2) {
                    System.out.println("Please enter a filename.");
                    System.exit(0);
                }
                Repository.rm(args[1]);
                break;
            case "log":
                Repository.log();
                break;
            default :
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
}
