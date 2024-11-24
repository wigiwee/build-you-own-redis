import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler {

    private Socket clientSocket;
    public final static String CRLF = "\r\n";
    static ConcurrentHashMap<String, String> keyValueHashMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Long> keyExpiryHashMap = new ConcurrentHashMap<>();

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    static int stringEncoding(InputStream fis) throws IOException {
        int length = 0;
        int b = fis.read();
        int first2Byte = b & 11000000;
        if (first2Byte == 0x00) {
            length = b & 00111111;
        } else if (first2Byte == 0xC0) {
            length = 8;
        } else if (first2Byte == 0xC1) {

        } else if (first2Byte == 0xC2) {

        } else if (first2Byte == 0xC3) {
            // LZF algorithm

        }

        return length;
    }

    static int sizeEncoding(InputStream inputStream) throws IOException {
        // int b = fis.read(); //reading first byte
        // int length = 00;
        // int first2bits = b & 11000000;
        // if(first2bits == 00000000){
        // length = b;
        // }else if(first2bits == 01000000){
        // int nextByte = fis.read();
        // int lsb6 = b & 00111111;
        // int shiftby6bits = lsb6 << 8; //shift by 8 bits to make space of next 8 bits
        // of second byte;
        // length = shiftby6bits | (nextByte & 0xFF);
        // }else if(first2bits == 10000000){ //combining next 4 bytes to form the length
        // length = ((fis.read() & 0xFF) << 24) |
        // ((fis.read() & 0xFF) << 16) |
        // ((fis.read() & 0xFF) << 8) |
        // ((fis.read() & 0xFF));
        // }
        // return length;
        int read;

        read = inputStream.read();

        int len_encoding_bit = (read & 0b11000000) >> 6;

        int len = 0;

        // System.out.println("bit: " + (read & 0x11000000));

        if (len_encoding_bit == 0) {

            len = read & 0b00111111;

        } else if (len_encoding_bit == 1) {

            int extra_len = inputStream.read();

            len = ((read & 0b00111111) << 8) + extra_len;

        } else if (len_encoding_bit == 2) {

            byte[] extra_len = new byte[4];

            inputStream.read(extra_len);

            len = ByteBuffer.wrap(extra_len).getInt();

        }

        return len;
    }

    static String encodeArray(String[] inputArray) {
        StringBuilder output = new StringBuilder("");
        output.append("*").append(inputArray.length).append(CRLF);
        for (int i = 0; i < inputArray.length; i++) {
            output.append("$").append(inputArray[i].length()).append(CRLF).append(inputArray[i]).append(CRLF);
        }
        return output.toString();
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
                        keyValueHashMap.put(args[1], args[2]);
                        if (numArgs > 3) {
                            if (args[3].equalsIgnoreCase("px")) {
                                keyExpiryHashMap.put(args[1], System.currentTimeMillis() + Long.parseLong(args[4]));
                            }
                        }
                        writer.write("+OK\r\n");
                        writer.flush();

                    } else if (args[0].equalsIgnoreCase("config")) {
                        if (args[1].equalsIgnoreCase("get")) {
                            if (args[2].equalsIgnoreCase("dir")) {
                                writer.write(encodeArray(new String[] { "dir", Main.dir }));
                                writer.flush();
                            } else if (args[2].equalsIgnoreCase("dbfilename")) {
                                writer.write(encodeArray(new String[] { "dbfilename", Main.dbfilename }));
                                writer.flush();
                            } else {
                                writer.write("-ERROR: Unknown configuration key arguments\r\n");
                                writer.flush();
                            }
                        } else {
                            writer.write("-ERROR: Unknown command or incorrect arguments\r\n");
                            writer.flush();
                        }
                    } else if (args[0].equalsIgnoreCase("keys")) {
                        if (Main.dir.isEmpty() || Main.dbfilename.isEmpty()) {
                            writer.write("-ERROR: Unknown command or incorrect arguments\r\n");
                            writer.flush();
                        }
                        try (InputStream fis = new FileInputStream(new File(Main.dir, Main.dbfilename))) {
                            byte[] redis = new byte[5];
                            byte[] version = new byte[4];

                            fis.read(redis);
                            fis.read(version);

                            System.out.println("Magic String: " + new String(redis, StandardCharsets.UTF_8));
                            System.out.println("Magic String: " + new String(version, StandardCharsets.UTF_8));

                            int bytee;
                            while ((bytee = fis.read()) != -1) {
                                System.out.println("I am here");
                                if (bytee == 0xFB) {
                                    System.out.println("I am in FB");
                                    int hastTableSize = sizeEncoding(fis);
                                    int exipryKeyHashTable = sizeEncoding(fis);
                                    for (int i = 0; i < hastTableSize; i++) {
                                        if (fis.read() == 0xFC) {
                                            byte[] expiryTimeUnixFormat = new byte[8];
                                            fis.read(expiryTimeUnixFormat);
                                        }
                                        if (fis.read() == 0xFD) {
                                            byte[] expiryTime = new byte[4];
                                            fis.read(expiryTime);
                                        }
                                        int valueType = fis.read();
                                        // System.out.println("valuetype: " + valueType);
                                        int keyLength = fis.read();
                                        byte[] key = new byte[keyLength];
                                        String keyStr = new String(key);
                                        System.out.println(keyStr);
                                        // System.out.println("key: " + new String(key));
                                        fis.read(key);
                                        writer.write(encodeArray(new String[] { keyStr}));
                                        
                                    }

                                }
                            }
                        }
                        // if (args[1].equalsIgnoreCase("*")) {
                        //     String[] keys = (String[]) keyValueHashMap.keySet().toArray();
                        //     System.out.println(Arrays.toString(keys));
                        //     writer.write(encodeArray(keys));
                        //     writer.flush();
                        //     ;

                        // }

                    } else if (args[0].equalsIgnoreCase("get") && numArgs == 2) {
                        if (keyValueHashMap.contains(args[1])) {
                            if (keyExpiryHashMap.contains(args[1])) {
                                if (System.currentTimeMillis() < keyExpiryHashMap.get(args[1])) {
                                    writer.write("$" + keyValueHashMap.get(args[1]).length() + CRLF
                                            + keyValueHashMap.get(args[1]) + CRLF);
                                    writer.flush();
                                } else {
                                    keyExpiryHashMap.remove(args[1]);
                                    keyValueHashMap.remove(args[1]);
                                }
                            } else {
                                writer.write("$" + keyValueHashMap.get(args[1]).length() + CRLF
                                        + keyValueHashMap.get(args[1]) + CRLF);
                                writer.flush();
                            }
                        } else {
                            writer.write("$-1\r\n");
                            writer.flush();
                        }

                    } else if (args[0].equalsIgnoreCase("echo") && numArgs == 2) {
                        String message = args[1];
                        writer.write("$" + message.length() + CRLF + message + CRLF);
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