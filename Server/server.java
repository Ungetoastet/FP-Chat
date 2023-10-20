import java.net.*;
import java.io.*;
import java.util.*;

public class server {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(1870)) {
            System.out.println("Waiting for connections...");
            boolean running = true;

            MessageHandler msgHandler = new MessageHandler();
            
            while (running) {
                ServerThread thread = new ServerThread(server.accept(), msgHandler);
                thread.start();
                msgHandler.register_client(thread);
            }
        } 
        catch (IOException e) {
            System.out.println("Error in server - setup:\n" + e.toString());
        }
    }
    
    static String readString() {
        try (Scanner scanner = new Scanner(System.in)) {
            String s = scanner.nextLine();
            return s;
        }
    }
}

class ServerThread extends Thread {
    
    Socket client;
    MessageHandler msgHandler;

    ServerThread (Socket client, MessageHandler msgHandler) {
        this.client = client;
        this.msgHandler = msgHandler;
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Client connected!");
            
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            
            // read a newline or carriage return delimited string
            DataInputStream din = new DataInputStream(in);
            DataOutputStream dout = new DataOutputStream(out);

            msgHandler.push_message(this, "New client joined.");
            dout.writeUTF("You are now connected to the server.");
            
            while (client.isConnected()) {
                String usrmsg = din.readUTF();
                System.out.println("Recieved message: " + usrmsg);
                msgHandler.push_message(this, usrmsg);
            }
            
            msgHandler.deregister_client(this);
            System.out.println("Client disconnected, shutting down thread.");
        }
        
        catch (Exception e) {
            msgHandler.deregister_client(this);
            System.out.println("Error in server thread - main:\n" + e.toString());
        }
    } 

    public void send_message(String message) {
        try {
            OutputStream out = client.getOutputStream();
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeUTF("Message by other user: " + message);
        }
        catch (Exception e) {
            System.out.println("Error in server thread - send_message:\n" + e.toString());
        }
    }
}

class MessageHandler extends Thread{
    LinkedList<ServerThread> client_threads;

    MessageHandler() {
        this.client_threads = new LinkedList<>();
    }

    public void register_client(ServerThread client) {
        this.client_threads.add(client);
    }

    public void deregister_client(ServerThread client) {
        this.client_threads.remove(client);
    }

    public void push_message(ServerThread sender, String message) {
        for (ServerThread client : client_threads) {
            if (client == sender) {
                continue;  // Dont send the message back to the sender
            }
            
            client.send_message(message);
        }
    }
}