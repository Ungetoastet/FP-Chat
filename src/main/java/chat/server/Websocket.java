package chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Websocket {
    Socket socket;

    public Websocket(Socket socket) {
        this.socket = socket;
    }

    public void send_message(String message) throws IOException {
        int chunkSize = 65000;

        List<String> dataSplit = splitString(message, chunkSize);
        for (int i = 0; i < dataSplit.size() - 1; i++) {
            send_raw_message("DAT<|>" + dataSplit.get(i));
        }
        send_raw_message("END<|>" + dataSplit.get(dataSplit.size() - 1));
    }

    private List<String> splitString(String input, int chunkSize) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < input.length(); i += chunkSize) {
            result.add(input.substring(i, Math.min(i + chunkSize, input.length())));
        }
        return result;
    }

    public void send_raw_message(String message) throws IOException {
        send_raw_message(message, 1, true);
    }

    public void send_raw_message(String message, int opcode, boolean fin) throws IOException {
        OutputStream out = socket.getOutputStream();
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        int payloadLength = messageBytes.length;
        int payload_declearation_length;
        if (payloadLength <= 125) {
            payload_declearation_length = 1;
        }
        else if (payloadLength <= 0xFFFF) {
            payload_declearation_length = 3;
        }
        else {
            payload_declearation_length = 9;
        }
        byte[] frame = new byte[1 + payload_declearation_length + messageBytes.length];

        // Fin bit: 1
        frame[0] = (byte) (opcode);
        if (fin) {
            frame[0] += 1 << 7;
        }

        // Payload length (7 bits)
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
        System.arraycopy(messageBytes, 0, frame, 1+payload_declearation_length, messageBytes.length);

        out.write(frame);
    }

    public String recieve_websocket_message() throws IOException {
        StringBuilder msg = new StringBuilder();
        boolean end = false;
        while (!end) {
            String newmsg = this.recieve_raw_websocket_message();
            if (newmsg.split("<\\|>")[0].equals("END")) {
                end = true;
            }
            msg.append(newmsg.substring(6));
        }
        return msg.toString();
    }

    public String recieve_raw_websocket_message() throws IOException {
        InputStream in = socket.getInputStream();

        // Read the first two bytes to determine the frame type and payload length
        int firstByte = in.read();
        if (firstByte == -1) {
            socket.close();
            return "CLOSED";
        }
        int secondByte = in.read();

        // Determine the opcode (frame type)
        int opcode = firstByte & 0b00001111;

        // Determine fin bit
        boolean fin = (firstByte & 0b1000000) != 0;

        // Determine if the frame is masked
        boolean isMasked = (secondByte & 0b10000000) != 0;

        // Read the payload length
        int payloadLength = secondByte & 0b01111111;
        if (payloadLength == 126) { // The next two bytes represent the payload length
            payloadLength = in.read() << 8;
            payloadLength += in.read();
        }
        else if (payloadLength == 127) {
            // The next 8 bytes represent the payload length
            payloadLength = 0;
            for (int i = 7; i != 0; i--) {
                payloadLength += in.read() << (8 * i);
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
        String decodedData = new String(payloadData, StandardCharsets.UTF_8);
        switch (opcode) {
            case 0x0: // Continuation frame
                if (!fin) { // If this is not the final frame, read the next frame
                    decodedData += recieve_websocket_message();
                }
                return decodedData;
            case 0x1: // Text frame, return message
                return decodedData;
            case 0x8: // Closing frame
                socket.close();
                return "CLOSED";
            case 0x9: // Ping, respond with pong
                send_raw_message(decodedData, 0xA, true);
                return recieve_websocket_message();
            default:
                throw new IOException("Unknown opcode recieved: " + opcode);
        }
    }

    public void recieve_handshake() throws IOException, NoSuchAlgorithmException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
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
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                    .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                            .getBytes(StandardCharsets.UTF_8)))
                    + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            out.write(response, 0, response.length);
        }
    }
}
