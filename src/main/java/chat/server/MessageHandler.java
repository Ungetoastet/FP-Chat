package chat.server;

import java.io.*;
import java.util.LinkedList;
import java.util.logging.Logger;

class MessageHandler extends Thread {
    FrontendThread serverfrontend;
    LinkedList<ServerThread> client_threads;
    LinkedList<Account> registered_users;
    Logger logger;

    private final String accounts_filename = "registered_accounts.acc";


    MessageHandler() {
        this.logger = Logger.getLogger("mainLogger");
        this.client_threads = new LinkedList<>();
        this.registered_users = loadAccounts();
    }

    public void register_account(Account account) {
        this.registered_users.add(account);
        serverfrontend.update_registered();
        saveAccounts();
        logger.info("Registered user: " + account.name);
    }
    public void delete_account(Account account) {
        this.registered_users.remove(account);
        saveAccounts();
        logger.info("Deleted user: " + account.name);
    }

    public void register_client(ServerThread client) {
        this.client_threads.add(client);
        logger.info("Client from " + client.client.getInetAddress() + " logged in with account: " + client.account.name);
    }

    public void deregister_client(ServerThread client) {
        this.client_threads.remove(client);
        logger.info("Deregistered client from: " + client.client.getInetAddress());
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
        if (sender != null) {
            logger.info("Pushed message from " + sender.account.name + "@" + sender.client.getInetAddress() + ": " + message);
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

        StringBuilder s = new StringBuilder();
        for (Account account : activeAccounts()) {
            if (account == null) { return ""; }
            s.append(account.name).append(", ");
        }
        return s.toString();
    }

    public String getConnectedInfo() {
        String connected = getActiveAccountList().replace(", ", "|");
        return connected;
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

    private void saveAccounts() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(this.accounts_filename))) {
            out.writeObject(this.registered_users);
            logger.info("Saved account data to disk");
        }
        catch (Exception e) {
            System.out.println("Error when saving accounts:\n");
            logger.severe("Error when trying to save accounts!");
            e.printStackTrace();
        }
    }

    public LinkedList<Account> loadAccounts() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(this.accounts_filename))) {
            logger.info("Loaded account list from disk");
            return (LinkedList<Account>) in.readObject();
        }
        catch (FileNotFoundException notFoundException) {
            System.out.println("Warning: Couldnt find Account safe data, creating new...");
            logger.warning("Couldnt find Account safe data on disk, creating new...");
            return new LinkedList<Account>();
        }
        catch (Exception e){
            System.out.println("Error in Account loading:\n");
            e.printStackTrace();
            logger.severe("Error in Account loading: " + e);
            return new LinkedList<Account>();
        }
    }
}
