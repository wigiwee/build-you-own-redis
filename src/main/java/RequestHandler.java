import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

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
                String[] contentArray = content.split(" ");
                System.out.println("content: " + content);
                if (contentArray[0].equalsIgnoreCase("echo")) {
                    writer.write(reader.readLine());
                    writer.flush();
                }
                if (contentArray[0].equalsIgnoreCase("ping")) {
                    writer.write("+PONG\r\n");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
