package chat.server;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


public class main {
    static MessageHandler msgHandler;
    static ServerSocket server;

    public static void main(String[] args) {
        try {
            server = new ServerSocket(1870);
            System.out.println("Server running on " + server.getInetAddress() + ":" + server.getLocalPort());
            System.out.println("Waiting for connections...");
            boolean accept_new_connections = true;

            // Create message handler
            msgHandler = new MessageHandler();

            // Create and start server manager
            ServerManager manager = new ServerManager(msgHandler);
            manager.start();

            // Connect new clients
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
