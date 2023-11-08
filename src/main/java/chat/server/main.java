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

            // Automatically open the frontend
            try {
                String absolut_project_path = System.getProperty("user.dir").replaceAll("\\\\", "/");
                String relative_path = "/src/main/java/chat/server/frontend/server.html";
                java.awt.Desktop.getDesktop().browse(new java.net.URI(absolut_project_path+relative_path));
                logger.info("Opened server frontend");
            } catch (IOException | URISyntaxException e) {
                System.out.println("Error in server frontend - startup:\n");
                logger.severe("Error opening server frontend!");
                e.printStackTrace();
            }
            // Create server manager
            ServerManager manager = new ServerManager(roomManager);

            // Start server manager frontend server and wait for connection
            ServerSocket frontendServer = new ServerSocket(1871);
            FrontendThread frontendThread = new FrontendThread(frontendServer.accept(), roomManager, manager);

            roomManager.register_frontend(frontendThread);
            roomManager.newRoom("Hauptchat");
            roomManager.newRoom("Nebenchat");
            roomManager.newRoom("Kacke");
            frontendThread.start();
            logger.info("Server frontend connected");

            // Start server manager
            logger.info("Started server manager");
            manager.start();

            // Connect new clients
            while (accept_new_connections) {
                ServerThread thread = new ServerThread(server.accept(), roomManager);
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
        roomManager.getRooms().get(0).push_message(null, "SERVER<|>Server wurde runtergefahren");
        Logger logger = Logger.getLogger("mainLogger");
        for (ServerThread client : roomManager.getRooms().get(0).getClientThreads()) {
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
