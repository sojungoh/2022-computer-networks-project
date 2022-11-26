import java.io.IOException;
import java.net.*;

public class Server {

    private final ServerSocket mainSocket;
    private final ServerSocket subSocket;

    public Server(ServerSocket mainSocket, ServerSocket subSocket) {
        this.mainSocket = mainSocket;
        this.subSocket = subSocket;
    }

    public void startServer() {
        try {
            Thread thread;

            while(!mainSocket.isClosed() && !subSocket.isClosed()) {

                Socket mainSocket = this.mainSocket.accept();
                //Socket subSocket = this.subSocket.accept();
                ClientHandler clientHandler = new ClientHandler(mainSocket, this.subSocket);

                thread = new Thread(clientHandler);
                thread.start();

            }

        } catch (IOException e) {
            closeServerSocket();
        }
    }

    public void closeServerSocket() {
        try {

            if(mainSocket != null)
                mainSocket.close();
            if(subSocket != null)
                subSocket.close();

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

        ServerSocket mainSocket = new ServerSocket(mainPort);
        ServerSocket subSocket = new ServerSocket(subPort);
        Server server = new Server(mainSocket, subSocket);
        server.startServer();
    }

}
