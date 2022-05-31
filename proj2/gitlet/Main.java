package gitlet;

import java.util.Objects;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Ethan
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

        if (Objects.equals(firstArg, "init")) {
            if (Repository.isInitialized()) {
                System.out.println("A Gitlet version-control system already exists in the current directory.");
                System.exit(0);
            }
            Repository.init();
        } else {
            if (!Repository.isInitialized()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            switch (firstArg) {
                case "add" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a filename.");
                        System.exit(0);
                    }
                    Repository.add(args[1]);
                }
                case "commit" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a commit message.");
                        System.exit(0);
                    } else if (args.length > 2) {
                        System.out.println("Please quote your message.");
                        System.exit(0);
                    }
                    Repository.commit(args[1]);
                }
                case "rm" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a filename.");
                        System.exit(0);
                    }
                    Repository.rm(args[1]);
                }
                case "log" -> Repository.log();
                case "global-log" -> Repository.globalLog();
                case "find" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a message to find.");
                        System.exit(0);
                    }
                    Repository.find(args[1]);
                }
                case "status" -> Repository.status();
                case "checkout" -> {
                    if (args.length == 3) {
                        if (!Objects.equals(args[1], "--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repo.checkout(args[2], null, null);
                    } else if (args.length == 4) {
                        if (!Objects.equals(args[2], "--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repo.checkout(args[3], args[1], null);
                    } else if (args.length == 2) {
                        Repo.checkout(null, null, args[1]);
                    } else {
                        System.out.println("Please enter a correct command.");
                        System.exit(0);
                    }
                }
                case "branch" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a branch name.");
                        System.exit(0);
                    }
                    Repository.branch(args[1]);
                }
                case "rm-branch" -> {
                    if (args.length < 2) {
                        System.out.println("Please enter a branch name.");
                        System.exit(0);
                    }
                    Repository.rmBranch(args[1]);
                }
                default -> {
                    System.out.println("No command with that name exists.");
                    System.exit(0);
                }
            }
        }
    }
}
