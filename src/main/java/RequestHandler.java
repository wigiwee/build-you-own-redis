import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class RequestHandler {

    private Socket clientSocket;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));) {
            String content;
            while ((content = reader.readLine()) != null) {
                // Parse the RESP array
                if (content.startsWith("*")) {
                    int numArgs = Integer.parseInt(content.substring(1));
                    String[] args = new String[numArgs];
                    for (int i = 0; i < numArgs; i++) {
                        String lengthLine = reader.readLine();
                        if (!lengthLine.startsWith("$")) {
                            writer.write("-ERROR: Invalid RESP format\r\n");
                            writer.flush();
                            continue;
                        }
                        int length = Integer.parseInt(lengthLine.substring(1));
                        args[i] = reader.readLine(); 
                        if (args[i].length() != length) {
                            writer.write("-ERROR: Length mismatch\r\n");
                            writer.flush();
                            continue;
                        }
                    }
                    if (args[0].equalsIgnoreCase("ping")) {
                        writer.write("+PONG\r\n");
                        writer.flush();
                    }   
                    if (args[0].equalsIgnoreCase("echo") && numArgs == 2) {
                        String message = args[1];
                        writer.write("$" + message.length() + "\r\n" + message + "\r\n");
                        writer.flush();
                    } else {
                        writer.write("-ERROR: Unknown command or incorrect arguments\r\n");
                        writer.flush();
                    }
                } else {
                    writer.write("-ERROR: Invalid RESP input\r\n");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
