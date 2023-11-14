package chat.server;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

class ServerThread extends Thread {
    Websocket client;
    MessageHandler activeMsgHandler;
    RoomManager roomManager;
    Account account;
    Logger logger;

    ServerThread(Websocket client, RoomManager rm) {
        this.client = client;
        this.roomManager = rm;
        this.logger = Logger.getLogger("mainLogger");
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public void run() {
        try {
            // Setup streams and scanners
            logger.info("Client connected from " + client.socket.getInetAddress());

            client.recieve_handshake();
            logger.info("Successful handshake from " + client.socket.getInetAddress());

            // Login user
            boolean logged_in = false;
            while (!logged_in) {
                String loginmsg = wait_for_message();
                String[] login = loginmsg.split("<\\|>");
                if (login[0].equals("LOGIN")) {
                    for (Account account : roomManager.registered_users) {
                        if (account.name.equals(login[1]) && account.password.equals(login[2])) {
                            if (!account.allowed) {
                                send_message("LOGIN<|>BANNED");
                                break;
                            }
                            this.account = account;
                            logged_in = true;
                            activeMsgHandler = roomManager.rooms.get(0);
                            break;
                        }
                    }
                    if (!logged_in) {
                        send_message("LOGIN<|>WRONG");
                    }
                }
                if (login[0].equals("REGISTER")) {
                    this.account = new Account(login[1], login[2]);
                    boolean reg_success = roomManager.register_account(this.account);
                    if (!reg_success) {
                        send_message("LOGIN<|>DUPLICATE");
                        continue;
                    }
                    activeMsgHandler = roomManager.rooms.get(0);
                    activeMsgHandler.push_message(this, "SERVER<|>" + account.name + " hat sich registriert");
                    logged_in = true;
                }
            }

            send_message("LOGIN<|>SUCCESS");
            activeMsgHandler.register_client(this);

            activeMsgHandler.serverfrontend.update_connected();

            update_rooms();
            greeting();

            // Message read loop
            while (!client.socket.isClosed()) {
                String msg = wait_for_message();
                if (!process_message(msg)) {
                    // push the message only if it wasnt a command
                    if (activeMsgHandler == null) {
                        continue;
                    }
                    activeMsgHandler.push_message(this, msg);
                }
            }
        } catch (IOException e) {
            System.out.println("Error in server - IO setup:\n" + e);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error in server - ALG setup:\n" + e);
        }
    }

    public void send_message(String message) {
        try {
            client.send_message(message);
        } catch (SocketException e) {
            try {
                client.socket.close();
                if (activeMsgHandler != null) {
                    activeMsgHandler.deregister_client(this);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Error in server thread - send_message\n" + e);
        }
    }

    String wait_for_message() {
        try {
            return client.recieve_websocket_message();
        } catch (SocketException e) {
            try {
                client.socket.close();
                if (activeMsgHandler != null) {
                    activeMsgHandler.deregister_client(this);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return "";
        } catch (Exception e) {
            System.out.println("Error in server thread - read_message:\n");
            e.printStackTrace();
            return "";
        }
    }

    public void disconnect() {
        try {
            activeMsgHandler.deregister_client(this);
            activeMsgHandler = null;
            client.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update_rooms() {
        send_message("ROOMS<|>" + roomManager.getRoomList(activeMsgHandler));
        send_message("PRIVATEROOMS<|>" + roomManager.getPrivateRoomList(activeMsgHandler, this));
        send_message("PRITARGETS<|>" + roomManager.getPossiblePrivateChatTargets(this.account));
    }

    private boolean process_message(String message) throws IOException {
        if (message.equals("CLOSE")) {
            activeMsgHandler.deregister_client(this);
            activeMsgHandler.serverfrontend.update_connected();
            client.socket.close();
            return true;
        }
        if (message.split("<\\|>")[0].equals("SWITCHROOM")) {
            String roomname = message.split("<\\|>")[1];
            MessageHandler newroom = roomManager.get_room_by_name(roomname);
            activeMsgHandler.deregister_client(this);
            activeMsgHandler = newroom;
            newroom.register_client(this);
            greeting();
            update_rooms();
            roomManager.serverfrontend.update_connected();
            return true;
        }
        else if (message.split("<\\|>")[0].equals("SWITCHPRIROOM")) {
            String partner = message.split("<\\|>")[1];
            MessageHandler newroom = roomManager.get_private_room_by_names(this.account.name, partner);
            activeMsgHandler.deregister_client(this);
            activeMsgHandler = newroom;
            newroom.register_client(this);
            greeting();
            update_rooms();
            roomManager.serverfrontend.update_connected();
            return true;
        }
        else if (message.split("<\\|>")[0].equals("CREATEPRI")) {
            String partner = message.split("<\\|>")[1];
            MessageHandler newroom = roomManager.newPrivateRoom(this.account, roomManager.find_account_by_name(partner));
            activeMsgHandler.deregister_client(this);
            activeMsgHandler = newroom;
            newroom.register_client(this);
            greeting();
            update_rooms();
            roomManager.serverfrontend.update_connected();
            return true;
        }
        else if (message.split("<\\|>")[0].equals("DELETEPRI")) {
            String partner = message.split("<\\|>")[1];
            PrivateMessageHandler pm = roomManager.find_private_room_by_participants(this.account.name, partner);
            roomManager.deletePrivateRoom(pm);
            return true;
        }
        return false;
    }

    private void greeting() {
        String accts = activeMsgHandler.getActiveAccountList(this.account.name);
        String greeting = "SERVER<|>Du bist jetzt verbunden. ";
        if (accts.equals("")) {
             greeting += "Außer dir ist noch keiner hier!";
        }
        else {
            greeting += "Angemeldete Benutzer: ";
            greeting += accts;
        }
        send_message(greeting);
        update_connected_info();
    }

    public void update_connected_info() {
        send_message("CONNECTED<|>" + activeMsgHandler.getActiveAccountData(this.account.name));
    }
}
