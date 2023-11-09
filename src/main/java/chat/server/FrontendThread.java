package chat.server;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;

class FrontendThread extends Thread{
    Websocket websocket;
    MessageHandler mainMsgHandler;
    RoomManager roomManager;
    ServerManager manager;

    FrontendThread(Websocket websocket, RoomManager rm, ServerManager serverManager) {
        this.websocket = websocket;
        this.roomManager = rm;
        this.manager = serverManager;
        manager.connectFrontend(this);
    }

    @Override
    public void run() {
        this.mainMsgHandler = roomManager.rooms.get(0);
        Logger logger = Logger.getLogger("mainLogger");

        // Handshake for websocket upgrade
        try {
            websocket.recieve_handshake();
        }
        catch (Exception e) {
            logger.severe("Error in frontend handshake!");
            System.out.println("Error in frontend handshake:\n");
            e.printStackTrace();
        }

        update_registered();
        update_rooms();

        // Message read loop
        while (!websocket.socket.isClosed()) {
            String msg = wait_for_message();
            System.out.println("Nachricht empfangen, Länge: " + msg.length() + " Zeichen");
            if (msg.equals("CLOSED")) {
                try {
                    websocket.socket.close();
                    logger.info("Server closed");
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (msg.split("<\\|>")[0].equals("CONTROL")) {
                manager.process_command(msg.split("<\\|>", 2)[1]);
                logger.info("Recieved frontend command: " + msg);
                continue;
            }
            logger.info("Recieved frontend message: " + msg);
            String target = msg.split("<\\|>")[1];
            MessageHandler targetroom = roomManager.get_room_by_name(target);
            if (targetroom != null) {
                targetroom.push_message(null, "SERVER<|>" + msg.split("<\\|>")[2]);
            }
            else {
                ServerThread clitarget = roomManager.get_client_by_name(target);
                if (clitarget != null) {
                    clitarget.send_message("SERVER<|>" + msg.split("<\\|>")[2]);
                }
            }
        }
        logger.info("Frontend shutdown.");
    }

    public void send_message(String message) {
        try {
            websocket.send_message(message);
        } catch (SocketException e) {
            try {
                websocket.socket.close();
                roomManager.deregister_frontend();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Error in server thread - send_message\n" + e);
            Logger.getLogger("mainLogger").severe("Error in server frontend thread: " + e);
        }
    }

    String wait_for_message() {
        try {
            return websocket.recieve_websocket_message();
        }
        catch (SocketException e) {
            try {
                websocket.socket.close();
                roomManager.deregister_frontend();
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

    public void update_registered() {
        // Send registered clients
        String register_info = "REGISTERED<|>";
        register_info += roomManager.getAccountInfo();
        send_message(register_info);
    }

    public void update_connected() {
        String connected_info = "CONNECTED<|>";
        connected_info += roomManager.getConnectedInfo();
        send_message(connected_info);
    }

    public void update_rooms() {
        send_message("ROOMS<|>" + roomManager.getRoomList());
    }
}
