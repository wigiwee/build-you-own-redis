import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import configAndUtils.Config;
import configAndUtils.RdbUtils;
import configAndUtils.RequestHandler;
import configAndUtils.Roles;
import configAndUtils.Utils;

public class Main {

    public static void main(String[] args) throws UnknownHostException, IOException {

        Utils.readConfiguration(args);

        if (!Config.dir.isEmpty() && !Config.dbfilename.isEmpty()) {
            RdbUtils.processRdbFile();
        }

        
        if (Config.role.equals(Roles.SLAVE) && Config.isHandshakeComplete == false) {
            Thread doHandshake = new Thread(){
                public void run(){
                    Utils.handshake();
                }
            };
            doHandshake.start();
        }
        

        Config.printConfig();
        System.out.println("Logs from your program will appear here!");

        
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(Config.port);
            serverSocket.setReuseAddress(true);

            while (true) {

                clientSocket = serverSocket.accept();
                RequestHandler requestHandler = new RequestHandler(clientSocket);
                Thread.startVirtualThread(() -> {
                    requestHandler.run();
                });
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
