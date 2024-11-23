import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler {

    private Socket clientSocket;

    static ConcurrentHashMap<String, HashMapValue> hashMap = new ConcurrentHashMap<>();

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
                    System.out.println(Arrays.toString(args));

                    if (args[0].equalsIgnoreCase("ping")) {
                        writer.write("+PONG\r\n");
                        writer.flush();

                    } else if (args[0].equalsIgnoreCase("set") && numArgs >= 3) {
                        HashMapValue hashMapValue = new HashMapValue();
                        hashMapValue.value = args[2];
                        if (numArgs > 3) {
                            if (args[3].equalsIgnoreCase("px")) {
                                hashMapValue.expiry = System.currentTimeMillis() + Long.parseLong(args[4]);
                            }
                        } else {
                            hashMapValue.expiry = -1;
                        }
                        hashMap.put(args[1], hashMapValue);
                        writer.write("+OK\r\n");
                        writer.flush();

                    } else if (args[0].equalsIgnoreCase("get") && numArgs == 2) {
                        if (hashMap.containsKey(args[1])) {
                            HashMapValue obj = hashMap.get(args[1]);
                            if (System.currentTimeMillis() < obj.expiry) {
                                writer.write("$" + obj.value.length() + "\r\n" + obj.value + "\r\n");
                                writer.flush();
                            } else {
                                writer.write("$-1\r\n");
                                writer.flush();
                            }
                        } else {
                            writer.write("-ERROR: Unknown key\r\n");
                            writer.flush();
                        }

                    } else if (args[0].equalsIgnoreCase("echo") && numArgs == 2) {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close(); // Ensure socket is closed to avoid resource leaks
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}

class HashMapValue {
    String value;
    long expiry;
}