import java.net.*;
import java.io.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


public class server {
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
            System.out.println("Error in server - setup:\n" + e.toString());
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

class ServerManager extends Thread {
    MessageHandler msgHandler;
    String helpString =   "\nServer terminal help: \n"
                        + "EXIT                        Stops the server \n"
                        + "KICK <name>                 Kicks the user\n"
                        + "BAN <name>                  Bans the user and kicks them\n"
                        + "UNBAN <name>                Unbans the user \n"
                        + "REGISTER <name> <password>  Registers a new user \n"
                        + "RENAME <name> <new_name>    Changes the name of a user\n"
                        + "SETPASS <name> <new_pwd>    Changes the password of a user\n"
                        + "SAY <text>                  Sends a message to all clients\n"
                        + "HELP                        Show this page\n";

    ServerManager (MessageHandler msgHandler) {
        this.msgHandler = msgHandler;
    }

    @Override
    public void run() {
        System.out.println("Server Manager started");
        while (!server.server.isClosed()) {
            System.out.print("[SERVER] << ");
            String input = readString();

            String cmd = input.split(" ")[0].toUpperCase();
            String[] args = input.split(" ");

            switch (cmd) {
                case "EXIT":
                    System.out.println("[SERVER] >> Stopping server");
                    server.stop_server();
                    break;

                case "REGISTER":
                    msgHandler.registered_users.add(
                        new Account(args[1], args[2]));
                    break;

                case "BAN":
                    if (!banUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "KICK":
                    if (!kickUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;
                    
                case "UNBAN":
                    if (!unbanUser(args[1])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;
                    
                case "RENAME":
                    if (!rename(args[1], args[2])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;

                case "SETPASS":
                    if (!change_pw(args[1], args[2])) {
                        System.out.println("[SERVER] >> No user with name " + args[1] + " found.");
                    }
                    break;
                
                case "SAY":
                    msgHandler.push_message(null, "SERVER " + input.split(" ", 2)[1]);
                    break;
                
                case "HELP":
                    System.out.println(helpString);
                    break;
                    
                default:
                    System.out.println("[SERVER] >> Unknown command " + cmd + ". Type 'help' for help page");
            }
        }
    }

    boolean kickUser(String name) {
        for (ServerThread client : msgHandler.client_threads) {
            if (client.account.name.equals(name)) {
                client.disconnect();
                msgHandler.push_message(null, "SERVER Kicked " + name);
                return true;
            }
        }
        return false;
    }

    boolean banUser(String name) {
        // Disallow the account...
        Account target = find_account_by_name(name);
        if (target == null) { return false; }
        target.ban();

        // ... and terminate the connection (if there is one)
        kickUser(name);

        msgHandler.push_message(null, "SERVER Banned " + name);
        return true;
    }
    
    boolean unbanUser(String name) {
        // Allow the account...
        Account target = find_account_by_name(name);
        if (target == null) { return false; }
        target.unban();
        msgHandler.push_message(null, "SERVER Unbanned " + name);
        return true;
    }
    
    boolean rename(String name, String newname) {
        Account target = find_account_by_name(name);
        if (target == null) { return false; }
        target.name = newname;
        return true;
    }

    boolean change_pw(String name, String newpass) {
        Account target = find_account_by_name(name);
        if (target == null) { return false; }
        target.password = newpass;
        return true;
    }
    
    Account find_account_by_name(String name) {
        for (Account account : msgHandler.registered_users) {
            if (account.name.equals(name)) {
                return account;
            }
        }
        return null;
    }

    static String readString() {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        return s;
    }
}

class ServerThread extends Thread {
    Socket client;
    MessageHandler msgHandler;
    InputStream in;
    OutputStream out;
    Account account;

    ServerThread (Socket client, MessageHandler msgHandler) {
        this.client = client;
        this.msgHandler = msgHandler;
    }

    public Account getAccount() {
        return account;
    }
    
    @Override
    public void run() {
        try {
            // Setup streams and scanners
            System.out.println("Client connected!");
            
            this.in = client.getInputStream();
            this.out = client.getOutputStream();
            
            Scanner s = new Scanner(in, "UTF-8");

            // Handshake for websocket upgrade
            String data = s.useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET").matcher(data);
            if (get.find()) {
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                    + "\r\n\r\n").getBytes("UTF-8");
                out.write(response, 0, response.length);
            }

            System.out.println("Handshake successful!");
            
            // Login user
            boolean logged_in = false;
            while (!logged_in) {
                String loginmsg = wait_for_message();
                String[] login = loginmsg.split("<\\|>");
                if (login[0].equals("LOGIN")) {
                    for (Account account : msgHandler.registered_users) {
                        if (account.name.equals(login[1]) && account.password.equals(login[2])) {
                            if (!account.allowed) {
                                send_message("LOGIN BANNED");
                                break;
                            }
                            this.account = account;
                            logged_in = true;
                            msgHandler.push_message(this, "SERVER " + account.name + " ist beigetreten");
                            break;
                        }
                    }
                    if (!logged_in) {
                        send_message("LOGIN WRONG");
                    }
                }
                if (login[0].equals("REGISTER")) {
                    this.account = new Account(login[1], login[2]);
                    msgHandler.registered_users.add(this.account);
                    msgHandler.push_message(this, "SERVER " + account.name + " hat sich registriert");
                    logged_in = true;
                }
            }

            send_message("LOGIN SUCCESS");

            String greeting = "SERVER Du bist jetzt verbunden. Angemeldete Benutzer: ";
            greeting += msgHandler.getActiveAccountList();
            send_message(greeting);
            
            // Message read loop
            while (!client.isClosed()) {
                System.out.println("Waiting for data...");
                String msg = wait_for_message();
                msgHandler.push_message(this, msg);
                System.out.println("Recieved message: " + msg);
            }

            s.close();
            client.close();
        }
        catch (IOException e) {
            System.out.println("Error in server - IO setup:\n" + e.toString());
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Error in server - ALG setup:\n" + e.toString());
        }
    }
    
    public void send_message(String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[10 + messageBytes.length];
        
        // Fin bit: 1, Text frame opcode: 0x1
        frame[0] = (byte) 0b10000001;
        
        // Payload length (7 bits)
        int payloadLength = messageBytes.length;
        if (payloadLength <= 125) {
            frame[1] = (byte) payloadLength;
        } else if (payloadLength <= 0xFFFF) {
            frame[1] = (byte) 126;
            frame[2] = (byte) ((payloadLength >> 8) & 0xFF);
            frame[3] = (byte) (payloadLength & 0xFF);
        } else {
            // For large payloads (more than 2^16 bytes)
            frame[1] = (byte) 127;
            for (int i = 0; i < 8; i++) {
                frame[2 + i] = (byte) ((payloadLength >> ((7 - i) * 8)) & 0xFF);
            }
        }
        
        // Copy payload data into frame
        System.arraycopy(messageBytes, 0, frame, 2, messageBytes.length);
        
        try {
            // Send the frame
            out.write(frame);
        }
        catch (SocketException e) {
            try {
                client.close();
                msgHandler.deregister_client(this);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        catch (IOException e) {
            System.out.println("Error in server thread - send_message\n" + e.toString());
        }
    }

    String wait_for_message() {
        try {
            // Read the first two bytes to determine the frame type and payload length
            int firstByte = in.read();
            if (firstByte == -1) {
                return "";
            }
            int secondByte = in.read();
    
            // Determine the opcode (frame type)
            int opcode = firstByte & 0b00001111;
            if (opcode == 0x8) {
                client.close();
                return "";
            }

            // Determine if the frame is masked
            boolean isMasked = (secondByte & 0b10000000) != 0;
    
            // Read the payload length
            int payloadLength = secondByte & 0b01111111;
            if (payloadLength == 126) {
                payloadLength = (in.read() << 8) | in.read();
            } else if (payloadLength == 127) {
                // For large payloads (more than 2^16 bytes), the next 8 bytes represent the payload length
                payloadLength = 0;
                for (int i = 0; i < 8; i++) {
                    payloadLength |= (in.read() & 0xFF) << (56 - 8 * i);
                }
            }
    
            // Read the masking key if the frame is masked
            byte[] maskingKey = null;
            if (isMasked) {
                maskingKey = new byte[4];
                in.read(maskingKey);
            }
    
            // Read the payload data
            byte[] payloadData = new byte[payloadLength];
            in.read(payloadData);
    
            // Unmask the payload data if the frame is masked
            if (isMasked) {
                for (int i = 0; i < payloadLength; i++) {
                    payloadData[i] = (byte) (payloadData[i] ^ maskingKey[i % 4]);
                }
            }
    
            // Handle the payload data (e.g., convert it to a string)
            String decodedData = new String(payloadData, "UTF-8");
            return decodedData;
        }
        catch (SocketException e) {
            try {
                client.close();
                msgHandler.deregister_client(this);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return "";
        }
        catch (Exception e) {
            System.out.println("Error in server thread - read_message:\n");
            e.printStackTrace();
            return "";
        }
    }

    public void disconnect() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MessageHandler extends Thread{
    LinkedList<ServerThread> client_threads;
    LinkedList<Account> registered_users;

    MessageHandler() {
        this.client_threads = new LinkedList<>();
        this.registered_users = new LinkedList<>();
        this.registered_users.add(new Account("robert", "scheer"));
        this.registered_users.add(new Account("lucie", "wolf"));
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

        String s = "";
        for (Account account : activeAccounts()) {
            s += account.name + ", ";
        }
        return s;
    }

    public LinkedList<ServerThread> getClientThreads() {
        return client_threads;
    }
}

class Account {
    boolean allowed = true;
    String name;
    String password;

    Account(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public void ban() {
        this.allowed = false;
    }

    public void unban() {
        this.allowed = true;
    }

}