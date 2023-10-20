import java.net.*;
import java.io.*;
import java.util.*;

public class server{
    public static void main(String[] args) {
        boolean run = true;
        
        try {
            ServerSocket listener = new ServerSocket(1870);

            while(run) {
                System.out.println("Waiting for connection...");
                
                Socket client = listener.accept(); // wait for connection
                System.out.println("Client connected!");

                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                // read a newline or carriage return delimited string
                DataInputStream din = new DataInputStream(in);
                DataOutputStream dout = new DataOutputStream(out);

                while (client.isConnected()) {
                    String usrmsg = din.readUTF();
                    System.out.println("Recieved message: " + usrmsg);
                    
                    dout.writeUTF(usrmsg);
                }

                client.close();
            }

            System.out.println("Client disconnected, shutting down server.");
            listener.close();
        } 
        catch (Exception e) {}

    }

    static String readString() {
        try (Scanner scanner = new Scanner(System.in)) {
            String s = scanner.nextLine();
            return s;
        }
    }
}