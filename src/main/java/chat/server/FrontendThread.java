package chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FrontendThread extends Thread{
    Socket socket;
    MessageHandler msgHandler;
    OutputStream out;
    ServerManager manager;

    FrontendThread(Socket socket, MessageHandler msgHandler, ServerManager serverManager) {
        this.socket = socket;
        this.msgHandler = msgHandler;
        try {
            this.out = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.manager = serverManager;
        manager.connectFrontend(this);
    }

    @Override
    public void run() {
        Logger logger = Logger.getLogger("mainLogger");
        // Handshake for websocket upgrade
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // Setup streams and scanners
            Scanner s = new Scanner(in, StandardCharsets.UTF_8);
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
                        + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                out.write(response, 0, response.length);
            }
        }
        catch (Exception e) {
            logger.severe("Error in frontend handshake!");
            System.out.println("Error in frontend handshake:\n");
            e.printStackTrace();
        }

        update_registered();

        // Message read loop
        while (!socket.isClosed()) {
            String msg = wait_for_message();
            if (Objects.equals(msg.split("<\\|>")[0], "CONTROL")) {
                manager.process_command(msg.split("<\\|>", 2)[1]);
                logger.info("Recieved frontend command: " + msg);
                continue;
            }
            logger.info("Recieved frontend message: " + msg);
            msgHandler.push_message(null, msg);
        }
        logger.info("Frontend shutdown.");
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
                socket.close();
                msgHandler.deregister_frontend();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Error in server thread - send_message\n" + e.toString());
        }
    }

    String wait_for_message() {
        InputStream in;
        try {
            in = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                socket.close();
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
            return new String(payloadData, StandardCharsets.UTF_8);
        } catch (SocketException e) {
            try {
                socket.close();
                msgHandler.deregister_frontend();
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
        register_info += msgHandler.getAccountInfo();
        send_message(register_info);
    }

    public void update_connected() {
        String connected_info = "CONNECTED<|>";
        connected_info += msgHandler.getConnectedInfo();
        send_message(connected_info);
    }
}
