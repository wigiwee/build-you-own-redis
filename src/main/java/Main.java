import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    
    static String dir = "";
    static String dbfilename = "";
    static int port = 6379;
    static String hostname = "";
    static int hostPort = -1;

    public static void main(String[] args) {

        for(int i = 0 ; i < args.length; i+=2){
            if(args[i].equals("--dir")){
                dir= args[i+1];
            }else if( args[i].equals("--dbfilenam")){
                dbfilename = args[i+1];
            }else if(args[i].equals("--port")){
                port = Integer.parseInt(args[i+1]);
            }else if( args[i].equals("--replicaof")){
                String value = args[i+1];
                String trimQuotation = value.substring(1, value.length());
                String[] strArray = trimQuotation.split(" ");
                hostname =  strArray[0];
                hostPort = Integer.parseInt(strArray[1]);
            }
        }

        System.out.println("Logs from your program will appear here!");
        
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {

            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (true) {
    
                clientSocket = serverSocket.accept();
    
                RequestHandler requestHandler = new RequestHandler(clientSocket);
                Thread.startVirtualThread(() -> {
                    try {
                        requestHandler.run();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
