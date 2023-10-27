package chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FrontendThread extends Thread{
    Socket socket;
    MessageHandler msgHandler;

    FrontendThread(Socket socket, MessageHandler msgHandler) {
        this.socket = socket;
        this.msgHandler = msgHandler;
    }

    @Override
    public void run() {

        // Handshake for websocket upgrade
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // Setup streams and scanners
            System.out.println("Server frontend connected!");

            Scanner s = new Scanner(in, "UTF-8");
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
            System.out.println("Error in frontend handshake:\n");
            e.printStackTrace();
        }

        System.out.println("Server frontend handshake successful!");
    }
}
