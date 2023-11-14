package chat.server;

import java.io.*;
import java.util.LinkedList;
import java.util.logging.Logger;

public class RoomManager {
    LinkedList<MessageHandler> rooms;
    LinkedList<PrivateMessageHandler> private_rooms;
    LinkedList<Account> registered_users;
    LinkedList<ServerThread> connected_clients;
    Logger logger;
    FrontendThread serverfrontend;

    private final String accounts_filename = "registered_accounts.acc";

    public RoomManager() {
        this.logger = Logger.getLogger("mainLogger");
        this.rooms = new LinkedList<>();
        this.private_rooms = new LinkedList<>();
        this.connected_clients = new LinkedList<>();
        this.registered_users = loadAccounts();
    }

    public MessageHandler newRoom(String name) {
        MessageHandler ms = new MessageHandler(name, this.serverfrontend, this);
        rooms.add(ms);
        logger.info("Created new room: " + name);
        return ms;
    }

    public PrivateMessageHandler newPrivateRoom(Account p1, Account p2) {
        PrivateMessageHandler pmh = new PrivateMessageHandler(this.serverfrontend, this, p1, p2);
        private_rooms.add(pmh);
        logger.info("Created new private room: " + p1.name + "&" + p2.name);
        return pmh;
    }

    public void deleteRoom(String name) {
        MessageHandler del = get_room_by_name(name);
        int moveindex = 0;
        if (del == rooms.get(0)) {
            moveindex += 1;
        }
        for (ServerThread cli : del.client_threads) {
            del.deregister_client(cli);
            rooms.get(moveindex).register_client(cli);
            cli.activeMsgHandler = rooms.get(moveindex);
            cli.update_rooms();
        }
        rooms.remove(del);
        logger.info("Deleted room: " + name);
    }

    public LinkedList<MessageHandler> getRooms() {
        return rooms;
    }

    public String getRoomList() {
        return getRoomList(null);
    }

    public String getRoomList(MessageHandler active) {
        StringBuilder lst = new StringBuilder();
        for (MessageHandler room : this.rooms) {
            lst.append(room.getUserCount()).append("@");
            lst.append(room.getRoomName());
            if (room == active) {
                lst.append("/!!/");
            }
            lst.append("|");
        }
        String f = lst.toString();
        return f.substring(0, f.length() - 1);
    }

    public MessageHandler get_room_by_name(String name) {
        for (MessageHandler room : this.rooms) {
            if (room.getRoomName().equals(name)) {
                return room;
            }
        }
        return null;
    }

    public PrivateMessageHandler get_private_room_by_names(String nameA, String nameB) {
        Account p1 = find_account_by_name(nameA);
        Account p2 = find_account_by_name(nameB);
        for (PrivateMessageHandler pm : private_rooms) {
            if (pm.is_participant(p1) && pm.is_participant(p2)) {
                return pm;
            }
        }
        return null;
    }

    public boolean register_account(Account account) {
        if (this.find_account_by_name(account.name) != null) {
            return false;
        }
        this.registered_users.add(account);
        serverfrontend.update_registered();
        saveAccounts();
        logger.info("Registered user: " + account.name);
        return true;
    }
    public void delete_account(Account account) {
        this.registered_users.remove(account);
        saveAccounts();
        logger.info("Deleted user: " + account.name);
    }

    public String getAccountInfo() {
        StringBuilder s = new StringBuilder();
        for (Account acc : registered_users) {
            if (!acc.allowed) {
                s.append("/!!/");  // Add flag if user is banned
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

    public void register_frontend(FrontendThread newsfe) {
        this.serverfrontend = newsfe;
    }

    public void deregister_frontend() {
        this.serverfrontend = null;
    }

    public void register_client(ServerThread cli) {
        this.connected_clients.add(cli);
        update_client_room_list();
    }
    public void deregister_client(ServerThread cli) {
        this.connected_clients.remove(cli);
        update_client_room_list();
    }

    public String getConnectedInfo() {
        StringBuilder s = new StringBuilder();
        if (connected_clients.size() == 0) {
            return "";
        }
        for (ServerThread cli : connected_clients) {
            Account account = cli.account;
            if (account == null) { return ""; }
            s.append(account.name).append("@");
            s.append(cli.activeMsgHandler.getRoomName()).append("|");
        }
        String str = s.toString();
        String connected = str.substring(0, str.length() - 1);
        return connected;
    }

    public void update_client_room_list() {
        for (ServerThread cli : connected_clients) {
            cli.update_rooms();
        }
        if (serverfrontend != null) {
            serverfrontend.update_rooms();
        }
    }

    public void renameRoom(String oldname, String newname) {
        get_room_by_name(oldname).room_name = newname;
        update_client_room_list();
    }

    public ServerThread get_client_by_name(String name) {
        for (ServerThread cli : this.connected_clients) {
            if (cli.account.name.equals(name)) {
                return cli;
            }
        }
        return null;
    }

    public Account find_account_by_name(String name) {
        for (Account acc : this.registered_users) {
            if (acc.name.equals(name)) {
                return acc;
            }
        }
        return null;
    }

    public String getPrivateRoomList(MessageHandler active, ServerThread cli) {
        StringBuilder lst = new StringBuilder();
        for (PrivateMessageHandler room : this.private_rooms) {
            if (!(room.is_participant(cli.account))) {
                continue;
            }
            lst.append(room.get_partner_name(cli.account));
            if (room == active) {
                lst.append("/!!/");
            }
            lst.append("|");
        }
        String f = lst.toString();
        if (f.length() > 0) {
            return f.substring(0, f.length() - 1);
        }
        else {
            return "";
        }
    }

    public String getPossiblePrivateChatTargets(Account requestingAccount) {
        StringBuilder returnstring = new StringBuilder();
        for (Account acc : registered_users) {
            if (acc == requestingAccount) {
                continue;
            }
            boolean possible = true;
            for (PrivateMessageHandler pm : private_rooms) {
                if (pm.is_participant(acc) && pm.is_participant(requestingAccount)) {
                    possible = false;
                    break;
                }
            }
            if (possible) {
                returnstring.append(acc.name).append("|");
            }
        }
        if (returnstring.length() < 1) {
            return "";
        }
        String f = returnstring.toString();
        return f.substring(0, f.length() - 1);
    }
}
