package chat.server;

import java.util.LinkedList;
import java.util.logging.Logger;

class MessageHandler extends Thread {
    LinkedList<ServerThread> client_threads;
    LinkedList<String> message_history;
    String room_name;
    Logger logger;
    RoomManager roomManager;
    int userCount;

    MessageHandler(String name, RoomManager rm) {
        this.logger = Logger.getLogger("mainLogger");
        this.client_threads = new LinkedList<>();
        this.message_history = new LinkedList<>();
        this.room_name = name;
        this.roomManager = rm;
        this.userCount = 0;
    }

    public String getRoomName() {
        return room_name;
    }

    public void register_client(ServerThread client) {
        userCount += 1;
        this.client_threads.add(client);
        roomManager.register_client(client);
        logger.info(room_name + ": Client from " + client.client.socket.getInetAddress() + " logged in with account: " + client.account.name);
        update_client_connection_info();
        for (String past_message : message_history) {
            client.send_message(past_message);
        }
        push_message(null, "SERVER<|>" + client.account.name + " ist beigetreten");
    }

    public void deregister_client(ServerThread client) {
        userCount -= 1;
        this.client_threads.remove(client);
        push_message(null, "SERVER<|>" + client.account.name + " ist gegangen");
        roomManager.deregister_client(client);
        logger.info(room_name + ": Deregistered client from: " + client.client.socket.getInetAddress());
        update_client_connection_info();
    }

    public void push_message(ServerThread sender, String message) {
        if (client_threads.size() == 0) {
            logger.info("Tried to push message, but no clients are connected.");
            return;
        }
        for (ServerThread client : client_threads) {
            if (client == sender) {
                continue;  // Dont send the message back to the sender
            }
            client.send_message(message);
        }
        if (sender != null) {
            logger.info("Pushed message from " + sender.account.name + "@" + sender.client.socket.getInetAddress() + ": " + message);
            message_history.add(message);
        }
    }

    public Account[] activeAccounts() {
        Account[] accounts = new Account[client_threads.size()];
        int i = 0;
        for (ServerThread cli : client_threads) {
            accounts[i] = cli.getAccount();
            i += 1;
        }
        return accounts;
    }

    public String getActiveAccountData(String excludename) {
        return getActiveAccountList(excludename).replace(", ", "|");
    }

    public String getActiveAccountList() {
        return getActiveAccountList(null);
    }

    public String getActiveAccountList(String excludename) {
        if (client_threads.size() <= 1) {
            return "";
        }

        StringBuilder s = new StringBuilder();
        for (Account account : activeAccounts()) {
            if (account == null) { return ""; }
            if (account.name.equals(excludename)) {
                continue;
            }
            s.append(account.name).append(", ");
        }
        String str = s.toString();
        return str.substring(0, str.length() - 2);
    }

    public LinkedList<ServerThread> getClientThreads() {
        return client_threads;
    }

    public int getUserCount() {
        return userCount;
    }

    public void update_client_connection_info() {
        for (ServerThread cli : client_threads) {
            cli.update_connected_info();
        }
    }
}
