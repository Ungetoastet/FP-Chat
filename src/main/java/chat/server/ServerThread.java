package chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ServerThread extends Thread {
    Socket client;
    MessageHandler msgHandler;
    InputStream in;
    OutputStream out;
    Account account;

    ServerThread(Socket client, MessageHandler msgHandler) {
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
        } catch (IOException e) {
            System.out.println("Error in server - IO setup:\n" + e.toString());
        } catch (NoSuchAlgorithmException e) {
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
        } catch (SocketException e) {
            try {
                client.close();
                msgHandler.deregister_client(this);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
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
        } catch (SocketException e) {
            try {
                client.close();
                msgHandler.deregister_client(this);
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
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
