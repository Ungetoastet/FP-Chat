package chat.server;

import java.net.*;
import java.io.*;
import java.util.logging.*;


public class main {
    static ServerSocket server;
    static RoomManager roomManager;

    public static void main(String[] args) {
        Logger logger = Logger.getLogger("mainLogger");
        logger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        try {
            Handler fileHandler = new FileHandler("serverlog.log");
            System.out.println("Logger started.");
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.out.println("Error in server setup - logging:\n");
            e.printStackTrace();
        }

        roomManager = new RoomManager();
        logger.info("Started room manager");

        try {
            server = new ServerSocket(1870);
            logger.info("Server running on " + server.getInetAddress() + ":" + server.getLocalPort());
            boolean accept_new_connections = true;

            // Create server manager
            ServerManager manager = new ServerManager(roomManager);

            roomManager.newRoom("Hauptchat");
            roomManager.newRoom("Witziges Zeug");
            roomManager.newRoom("Ernstes Zeug");
            roomManager.newRoom("Tiere");
            roomManager.newRoom("Informatik");

            // Start server manager
            logger.info("Started server manager");
            manager.start();

            // Connect new clients
            while (accept_new_connections) {
                ServerThread thread = new ServerThread(new Websocket(server.accept()), roomManager);
                thread.start();
            }
        }
        catch (SocketException e) {
            logger.info("Server closed normally");
            System.out.println("Server shut down. Press Enter to continue...");
        }
        catch (IOException e) {
            System.out.println("Error in server - setup:\n" + e);
            logger.severe("Error in server setup: " + e);
        }
    }

    public static void stop_server() {
        Logger logger = Logger.getLogger("mainLogger");
        for (ServerThread client : roomManager.connected_clients) {
            client.send_message("SERVER<|>Server wurde runtergefahren");
            client.disconnect();
        }
        try {
            server.close();
            logger.info("Server shut down.");
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Error in server shutdown!");
        }
    }
}
