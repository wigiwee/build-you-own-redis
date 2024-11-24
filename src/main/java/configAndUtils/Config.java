package configAndUtils;

public class Config {
 

    public static String dir;

    public static String dbfilename;

    public static final String CRLF = "\r\n";

    public static final String NIL = "$-1" + CRLF;

    public static int port = 6379;

    public static String role = Roles.MASTER;

    public static String hostName = "";

    public static int hostPort = -1;

    public static String version = "";

}