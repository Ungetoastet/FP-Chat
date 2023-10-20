import java.net.*;
import java.io.*;
import java.util.*;


public class client{
    public static void main(String[] args) {
        try {
            Socket server = new Socket("localhost", 1870);
            InputStream in = server.getInputStream();
            OutputStream out = server.getOutputStream();

            // write a newline or carriage return delimited string
            DataInputStream din = new DataInputStream(in);
            DataOutputStream dout = new DataOutputStream(out);

            boolean connected = true;
            while (connected) {
                System.out.print(">> ");
                String user_text = readString();
                if (user_text.equals("exit")) {
                    connected = false;
                    continue;
                }
                dout.writeUTF(user_text);

                String response = din.readUTF(din);
                System.out.println("Server response: " + response);
            }

            server.close();
        }

        catch(UnknownHostException e) {
            System.out.println("Can't find host.");
        } 

        catch (IOException e) {
            System.out.println("Error connecting to host: " + e.toString()); 
        }
    }




    static String readString() {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        return s;
    }
}