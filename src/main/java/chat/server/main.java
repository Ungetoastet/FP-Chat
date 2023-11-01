package chat.server;

import java.net.*;
import java.io.*;

public class main {
    static MessageHandler msgHandler;
    static ServerSocket server;

    public static void main(String[] args) {
        try {
            server = new ServerSocket(1870);
            System.out.println("Server running on " + server.getInetAddress() + ":" + server.getLocalPort());
            boolean accept_new_connections = true;

            // Create message handler
            msgHandler = new MessageHandler();

            // Create and start server manager
            ServerManager manager = new ServerManager(msgHandler);
            manager.start();

            // Automatically open the frontend
            try {
                String absolut_project_path = System.getProperty("user.dir").replaceAll("\\\\", "/");
                String relative_path = "/src/main/java/chat/server/frontend/server.html";
                java.awt.Desktop.getDesktop().browse(new java.net.URI(absolut_project_path+relative_path));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Error in server frontend - startup:\n");
                e.printStackTrace();
            }
            // Start server manager frontend server and wait for connection
            ServerSocket frontendServer = new ServerSocket(1871);
            FrontendThread frontendThread = new FrontendThread(frontendServer.accept(), msgHandler, manager);
            frontendThread.start();

            msgHandler.register_frontend(frontendThread);

            // Connect new clients
            System.out.println("Waiting for connections...");
            while (accept_new_connections) {
                ServerThread thread = new ServerThread(server.accept(), msgHandler);
                thread.start();
                msgHandler.register_client(thread);
            }
        }
        catch (SocketException e) {
            System.out.println("Server shut down.");
        }
        catch (IOException e) {
            System.out.println("Error in server - setup:\n" + e);
        }
    }

    public static void stop_server() {
        msgHandler.push_message(null, "SERVER Server wurde runtergefahren");
        for (ServerThread client : msgHandler.getClientThreads()) {
            client.disconnect();
        }
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
