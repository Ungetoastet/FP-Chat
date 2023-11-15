package chat.server;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

class ServerManager extends Thread {
    MessageHandler mainMsgHandler;
    RoomManager roomManager;
    FrontendThread frontendThread;
    Logger logger;

    String helpString = "\nServer terminal help: \n"
            + "EXIT                        Stops the server \n"
            + "KICK|<name>                 Kicks the user\n"
            + "BAN|<name>                  Bans the user and kicks them\n"
            + "UNBAN|<name>                Unbans the user \n"
            + "REGISTER|<name>|<password>  Registers a new user \n"
            + "RENAME|<name>|<new_name>    Changes the name of a user\n"
            + "SETPASS|<name>|<new_pwd>    Changes the password of a user\n"
            + "SAY|<text>                  Sends a message to all clients\n"
            + "HELP                        Show this page\n";

    ServerManager(RoomManager rm) {
        this.roomManager = rm;
        this.logger = Logger.getLogger("mainLogger");
    }

    public void connectFrontend(FrontendThread frontendThread) {
        this.frontendThread = frontendThread;
    }

    @Override
    public void run() {
        this.mainMsgHandler = roomManager.rooms.get(0);
        logger.info("Server manager started");
        while (!main.server.isClosed()) {
            System.out.print("[SERVER] << ");
            String input = readString();
            process_command(input);
        }
    }

    public void process_command(String input) {
        String[] args = input.split("\\|");
        String cmd = args[0].toUpperCase();
        switch (cmd) {
            case "":
                break;
            case "EXIT":
                System.out.println("[SERVER] >> Stopping server");
                try {
                    frontendThread.websocket.socket.close();
                } catch (IOException e) {
                    System.out.println("Error in Server Manager - Frontend shutdown:\n");
                    e.printStackTrace();
                }
                main.stop_server();
                break;

            case "REGISTER":
                roomManager.registered_users.add(
                        new Account(args[1], args[2]));
                frontendThread.update_registered();
                break;

            case "BAN":
                if (!banUser(args[1])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                frontendThread.update_registered();
                frontendThread.update_connected();
                break;

            case "KICK":
                if (!kickUser(args[1])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                frontendThread.update_connected();
                break;

            case "UNBAN":
                if (!unbanUser(args[1])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                frontendThread.update_registered();
                break;

            case "DELETE":
                if (!deleteUser(args[1])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                frontendThread.update_connected();
                frontendThread.update_registered();
                break;

            case "RENAME":
                if (!rename(args[1], args[2])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                frontendThread.update_registered();
                break;

            case "SETPASS":
                if (!change_pw(args[1], args[2])) {
                    System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                }
                break;

            case "CREATEROOM":
                roomManager.newRoom(args[1]);
                frontendThread.update_rooms();
                roomManager.update_client_room_list();
                break;

            case "DELETEROOM":
                roomManager.deleteRoom(args[1]);
                frontendThread.update_rooms();
                roomManager.update_client_room_list();
                break;

            case "RENAMEROOM":
                roomManager.renameRoom(args[1], args[2]);
                frontendThread.update_rooms();
                frontendThread.update_connected();
                break;

            case "SAY":
                mainMsgHandler.push_message(null, "SERVER<|>" + input.split(" ", 2)[1]);
                break;

            case "HELP":
                System.out.println(helpString);
                break;

            default:
                System.out.println("[SERVER] >> Unknown command " + cmd + ". Type 'help' for help page");
        }
    }

    boolean kickUser(String name) {
        for (ServerThread client : roomManager.connected_clients) {
            if (client.account.name.equals(name)) {
                client.activeMsgHandler.push_message(null, "SERVER<|>Kicked " + name);
                client.disconnect();
                logger.info("Kicked user " + name);
                return true;
            }
        }
        return false;
    }

    boolean deleteUser(String name) {
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        kickUser(name);
        roomManager.delete_account(target);
        return true;
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

        mainMsgHandler.push_message(null, "SERVER<|>Banned " + name);
        return true;
    }

    boolean unbanUser(String name) {
        // Allow the account...
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        target.unban();
        mainMsgHandler.push_message(null, "SERVER<|>Unbanned " + name);
        return true;
    }

    boolean rename(String name, String newname) {
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        logger.info("Renamed user " + name + " to " + newname);
        target.name = newname;
        return true;
    }

    boolean change_pw(String name, String newpass) {
        Account target = find_account_by_name(name);
        if (target == null) {
            return false;
        }
        logger.info("Changed password of user " + name);
        target.password = newpass;
        return true;
    }

    Account find_account_by_name(String name) {
        for (Account account : roomManager.registered_users) {
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
