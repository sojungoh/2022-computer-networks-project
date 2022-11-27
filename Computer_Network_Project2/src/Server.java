import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {

    private final ServerSocket mainSocket;
    private final ServerSocket subSocket;

    public Server(ServerSocket mainSocket, ServerSocket subSocket) {
        this.mainSocket = mainSocket;
        this.subSocket = subSocket;
    }

    public void startServer() {

        System.out.println("Server starts.");

        try {
            Thread thread;

            while(!mainSocket.isClosed() && !subSocket.isClosed()) {

                Socket socket = mainSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, subSocket);

                System.out.println("A new client connected to the server.");
                thread = new Thread(clientHandler);
                thread.start();

            }

        } catch (IOException e) {
            closeServerSocket();
        }

        System.out.println("Server terminated.");

    }

    public void listenForCommand() {

        Scanner scan = new Scanner(System.in);

        new Thread(() -> {

            while(true) {
                String str = scan.nextLine();

                if(str.equals("#SHUTDOWN")) {
                    System.out.println("Do you really want to shutdown the server?(Y/N)");
                    str = scan.nextLine();

                    if(str.equals("Y") || str.equals("y")) {
                        closeServerSocket();
                        break;
                    }
                }
            }

        }).start();
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
        server.listenForCommand();
        server.startServer();

    }

}
