import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible
        // when running tests.
        System.out.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {

            while (true) {
                serverSocket = new ServerSocket(port);
                serverSocket.setReuseAddress(true);
    
                // Wait for connection from client.
                clientSocket = serverSocket.accept();
    
                RequestHandler requestHandler = new RequestHandler(clientSocket);
                Thread.startVirtualThread(requestHandler::run);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
