package chat.server;

import java.net.*;
import java.io.*;
import java.util.logging.*;


public class main {
    static MessageHandler msgHandler;
    static ServerSocket server;

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

        try {
            server = new ServerSocket(1870);
            logger.info("Server running on " + server.getInetAddress() + ":" + server.getLocalPort());
            boolean accept_new_connections = true;

            // Create message handler
            msgHandler = new MessageHandler();
            logger.info("Started message handler");

            // Create and start server manager
            ServerManager manager = new ServerManager(msgHandler);
            logger.info("Started server manager");
            manager.start();

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
            // Start server manager frontend server and wait for connection
            ServerSocket frontendServer = new ServerSocket(1871);
            FrontendThread frontendThread = new FrontendThread(frontendServer.accept(), msgHandler, manager);
            frontendThread.start();
            msgHandler.register_frontend(frontendThread);
            logger.info("Server frontend connected");

            // Connect new clients
            while (accept_new_connections) {
                ServerThread thread = new ServerThread(server.accept(), msgHandler);
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
        msgHandler.push_message(null, "SERVER<|>Server wurde runtergefahren");
        Logger logger = Logger.getLogger("mainLogger");
        for (ServerThread client : msgHandler.getClientThreads()) {
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
