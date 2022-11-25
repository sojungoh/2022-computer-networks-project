import java.util.*;
import java.io.*;
import java.net.*;

public class Client {

    private final Socket mainSocket;
    private final Socket subSocket;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;
    private String userName;

    public Client(Socket mainSocket, Socket subSocket, String name) throws IOException {
        this.mainSocket = mainSocket;
        this.subSocket = subSocket;
        this.bufferedReader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(mainSocket.getOutputStream()));
        this.userName = name;

        bufferedWriter.write(userName);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public void requestToServer(String command, String chatRoomName) {
        try {
            bufferedWriter.write(command);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            if(chatRoomName != null) {
                bufferedWriter.write(chatRoomName);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }

            if(command.equals("#EXIT"))
                closeEverything(mainSocket, bufferedReader, bufferedWriter);

        } catch (IOException e) {
            closeEverything(mainSocket, bufferedReader, bufferedWriter);
        }

    }

    public void sendMessage(String message) {
        try {

            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            closeEverything(mainSocket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage() {

        new Thread(() -> {
            String message;

            while(!mainSocket.isClosed()) {
                try {
                    message = bufferedReader.readLine();

                    if(message != null && !message.isEmpty())
                        System.out.println(message);

                } catch (IOException e) {
                    closeEverything(mainSocket, bufferedReader, bufferedWriter);
                }
            }
        }).start();
    }

    public void closeEverything(Socket mainSocket, BufferedReader reader, BufferedWriter writer) {
        try {
            if(reader != null)
                reader.close();
            if(writer != null)
                writer.close();
            if(mainSocket != null)
                mainSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        String serverIP;
        int mainPort, subPort;
        Client client = null;

        if(args.length != 3){
            System.out.println("You have to enter Server IP address, port 1, port 2");
            System.exit(0);
        }

        serverIP = args[0];
        mainPort = Integer.parseInt(args[1]);
        subPort = Integer.parseInt(args[2]);

        Scanner scan = new Scanner(System.in);
        Socket mainSocket = new Socket(serverIP, mainPort);
        Socket subSocket = new Socket(serverIP, subPort);

        label:
        while(true) {

            String str = scan.nextLine();

            if(str.isEmpty())
                continue;

            boolean isCommand = (str.charAt(0) == '#');

            if(isCommand) {

                String[] strArray = str.split("\\s");

                switch (strArray[0]) {
                    case "#CREATE":
                    case "#JOIN":

                        if (strArray.length != 3) {
                            System.out.println("You have to enter (chatroom name) and (user name).");
                            continue;
                        }

                        String chatRoomName = strArray[1];
                        String userName = strArray[2];

                        if (client == null) {
                            client = new Client(mainSocket, subSocket, userName);
                            client.listenForMessage();
                        }
                        client.requestToServer(strArray[0], chatRoomName);
                        break;
                    case "#PUT": {
                        if (strArray.length != 2) {
                            System.out.println("You have to enter (file name).");
                            continue;
                        }

                        String fileName = strArray[1];

                        break;
                    }
                    case "#GET": {
                        String fileName = strArray[1];
                        break;
                    }
                    case "#EXIT":
                        if (client != null)
                            client.requestToServer(strArray[0], null);
                        break label;
                    case "#STATUS":
                        if (client != null)
                            client.requestToServer(strArray[0], null);
                        else
                            System.out.println("You have to join a chatroom first.");
                        break;
                    default:
                        System.out.println("Unrecognized command.");
                        break;
                }
            }
            // message From client
            else {
                if(client != null) {
                    client.sendMessage(str);
                }
            }
        }
    }
}
