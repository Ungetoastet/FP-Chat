package chat.server;

import java.util.Scanner;

class ServerManager extends Thread {
    MessageHandler msgHandler;
    String helpString = "\nServer terminal help: \n"
            + "EXIT                        Stops the server \n"
            + "KICK <name>                 Kicks the user\n"
            + "BAN <name>                  Bans the user and kicks them\n"
            + "UNBAN <name>                Unbans the user \n"
            + "REGISTER <name> <password>  Registers a new user \n"
            + "RENAME <name> <new_name>    Changes the name of a user\n"
            + "SETPASS <name> <new_pwd>    Changes the password of a user\n"
            + "SAY <text>                  Sends a message to all clients\n"
            + "HELP                        Show this page\n";

    ServerManager(MessageHandler msgHandler) {
        this.msgHandler = msgHandler;
    }

    @Override
    public void run() {
        System.out.println("Server Manager started");
        while (!main.server.isClosed()) {
            System.out.print("[SERVER] << ");
            String input = readString();

            String cmd = input.split(" ")[0].toUpperCase();
            String[] args = input.split(" ");

            switch (cmd) {
                case "EXIT":
                    System.out.println("[SERVER] >> Stopping server");
                    main.stop_server();
                    break;

                case "REGISTER":
                    msgHandler.registered_users.add(
                            new Account(args[1], args[2]));
                    break;

                case "BAN":
                    if (!banUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "KICK":
                    if (!kickUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "UNBAN":
                    if (!unbanUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "RENAME":
                    if (!rename(args[1], args[2])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "SETPASS":
                    if (!change_pw(args[1], args[2])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "SAY":
                    msgHandler.push_message(null, "SERVER " + input.split(" ", 2)[1]);
                    break;

                case "HELP":
                    System.out.println(helpString);
                    break;

                default:
                    System.out.println("[SERVER] >> Unknown command " + cmd + ". Type 'help' for help page");
            }
        }
    }

    boolean kickUser(String name) {
        for (ServerThread client : msgHandler.client_threads) {
            if (client.account.name.equals(name)) {
                client.disconnect();
                msgHandler.push_message(null, "SERVER Kicked " + name);
                return true;
            }
        }
        return false;
    }

    boolean banUser(String name) {
        // Disallow the account...
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        target.ban();

        // ... and terminate the connection (if there is one)
        kickUser(name);

        msgHandler.push_message(null, "SERVER Banned " + name);
        return true;
    }

    boolean unbanUser(String name) {
        // Allow the account...
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        target.unban();
        msgHandler.push_message(null, "SERVER Unbanned " + name);
        return true;
    }

    boolean rename(String name, String newname) {
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        target.name = newname;
        return true;
    }

    boolean change_pw(String name, String newpass) {
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        target.password = newpass;
        return true;
    }

    Account find_account_by_name(String name) {
        for (Account account : msgHandler.registered_users) {
            if (account.name.equals(name)) {
                return account;
            }
        }
        return null;
    }

    static String readString() {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        return s;
    }
}