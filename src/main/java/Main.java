import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    
    static String dir = "";
    static String dbfilename = "";
    static int port = 6379;
    public static void main(String[] args) {

        if(args.length >=4 ){
            if(args[0].equals("--dir")){
                dir = args[1];
            }
            if (args[2].equals("--dbfilename")){
                dbfilename = args[3];
            }
        }
        for(int i = 0 ; i < args.length; i+=2){
            if(args[i].equals("--dir")){
                dir= args[i+1];
            }else if( args[i].equals("--dbfilenam")){
                dbfilename = args[i+1];
            }else if(args[i].equals("--port")){
                port = Integer.parseInt(args[i+1]);
            }
        }
        System.out.println(dir);
        System.out.println(dbfilename);

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
