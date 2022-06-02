package gitlet;

import java.util.Objects;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Ethan
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void checkValid() {
        if (!Repository.isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
    public static void main(String[] args) {
        String firstArg = null;
        try {
            firstArg = args[0];
        } catch (java.lang.ArrayIndexOutOfBoundsException exception) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        switch (firstArg) {
            case "init" -> {
                if (Repository.isInitialized()) {
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                }
                Repository.init();
            }
            case "add" -> {
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a filename.");
                    System.exit(0);
                }
                Repository.add(args[1]);
            }
            case "commit" -> {
                checkValid();
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
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a filename.");
                    System.exit(0);
                }
                Repository.rm(args[1]);
            }
            case "log" -> {
                checkValid();
                Repository.log();
            }
            case "global-log" -> {
                checkValid();
                Repository.globalLog();
            }
            case "find" -> {
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a message to find.");
                    System.exit(0);
                }
                Repository.find(args[1]);
            }
            case "status" -> {
                checkValid();
                Repository.status();
            }
            case "checkout" -> {
                checkValid();
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
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a branch name.");
                    System.exit(0);
                }
                Repository.branch(args[1]);
            }
            case "rm-branch" -> {
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a branch name.");
                    System.exit(0);
                }
                Repository.rmBranch(args[1]);
            }
            case "reset" -> {
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please enter a commit ID.");
                    System.exit(0);
                }
                Repo.reset(args[1]);
            }
            case "merge" -> {
                checkValid();
                if (args.length < 2) {
                    System.out.println("Please specify a branch to merge.");
                    System.exit(0);
                }
                Repo.merge(args[1]);
            }
            default -> {
                System.out.println("No command with that name exists.");
                System.exit(0);
            }
        }
    }
}
