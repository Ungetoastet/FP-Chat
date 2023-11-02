package chat.server;

import java.util.LinkedList;

class MessageHandler extends Thread {
    FrontendThread serverfrontend;
    LinkedList<ServerThread> client_threads;
    LinkedList<Account> registered_users;

    MessageHandler() {
        this.client_threads = new LinkedList<>();
        this.registered_users = new LinkedList<>();
        this.registered_users.add(new Account("robert", "scheer"));
        this.registered_users.add(new Account("lucie", "wolf"));
    }

    public void register_acocunt(Account account) {
        this.registered_users.add(account);
        serverfrontend.update_registered();
    }

    public void register_client(ServerThread client) {
        this.client_threads.add(client);
    }

    public void deregister_client(ServerThread client) {
        this.client_threads.remove(client);
    }

    public void register_frontend(FrontendThread newsfe) {
        this.serverfrontend = newsfe;
    }

    public void deregister_frontend() {
        this.serverfrontend = null;
    }

    public void push_message(ServerThread sender, String message) {
        if (serverfrontend != null) {
            serverfrontend.send_message(message);
        }
        for (ServerThread client : client_threads) {
            if (client == sender) {
                continue;  // Dont send the message back to the sender
            }
            client.send_message(message);
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

    public String getActiveAccountList() {
        if (client_threads.size() == 0) {
            return "";
        }

        String s = "";
        for (Account account : activeAccounts()) {
            if (account == null) { return ""; }
            s += account.name + ", ";
        }
        return s;
    }

    public LinkedList<ServerThread> getClientThreads() {
        return client_threads;
    }

    public String getAccountInfo() {
        StringBuilder s = new StringBuilder();
        for (Account acc : registered_users) {
            if (!acc.allowed) {
                s.append("/!!/");  // Add signs if user is banned
            }
            s.append(acc.name).append("|");
        }
        return s.toString();
    }
}
