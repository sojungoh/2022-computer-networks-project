import java.io.IOException;
import java.net.*;

public class Server {

    private ServerSocket socket;

    public Server(ServerSocket socket) {
        this.socket = socket;
    }

    public void startServer() {
        try {
            Thread thread;

            while(!socket.isClosed()) {
                Socket testSocket = socket.accept();
                ClientHandler clientHandler = new ClientHandler(testSocket);

                thread = new Thread(clientHandler);
                thread.start();

            }

        } catch (IOException e) {
            closeServerSocket();
        }
    }

    public void closeServerSocket() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        int mainPort, subPort;

        if(args.length != 2){
            System.out.println("You have to enter port 1, port 2");
            System.exit(0);
        }

        mainPort = Integer.parseInt(args[0]);
        subPort = Integer.parseInt(args[1]);

        ServerSocket socket = new ServerSocket(mainPort);
        Server server = new Server(socket);
        server.startServer();

    }

}
