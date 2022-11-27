import java.util.*;
import java.io.*;
import java.net.*;

public class Client {

    private final Socket mainSocket;
    private final String serverIP;
    private final int subPort;
    private final BufferedReader msgReader;
    private final BufferedWriter msgWriter;

    public Client(String serverIP, int mainPort, int subPort) throws IOException {

        this.mainSocket = new Socket(serverIP, mainPort);
        this.serverIP = serverIP;
        this.subPort = subPort;
        this.msgReader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
        this.msgWriter = new BufferedWriter(new OutputStreamWriter(mainSocket.getOutputStream()));

    }

    public void writeToBuffer(String msg) {
        try {

            msgWriter.write(msg);
            msgWriter.newLine();
            msgWriter.flush();

        } catch (IOException e) {
            closeResources();
        }
    }

    public void requestChatService(String command, String str1, String str2) {

        writeToBuffer(command);
        if(str1 != null)
            writeToBuffer(str1);
        if(str2 != null)
            writeToBuffer(str2);

    }

    public void listenForMessage() {

        new Thread(() -> {
            String message;

            while(!mainSocket.isClosed()) {
                try {

                    message = msgReader.readLine();

                    if(message != null && !message.isEmpty())
                        System.out.println(message);

                } catch (IOException e) {
                    closeResources();
                }
            }

        }).start();

    }

    public void sendFile(String fileName) {

        int readBytes;
        byte[] buffer = new byte[64*1024];  // 64KiB
        File file = new File(fileName);

        if(!file.exists()) {
            System.out.println("File name \"" + fileName + "\" does not exist.");
            return;
        }

        try {
            requestChatService("#PUT", fileName, null);

            Socket socket = new Socket(serverIP, subPort);
            BufferedOutputStream fileSender = new BufferedOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);

            System.out.println("----- sending the file -----");
            while((readBytes = fis.read(buffer)) > 0) {
                fileSender.write(buffer, 0, readBytes);
                fileSender.flush();
                if(readBytes == 64*1024)
                    System.out.print("#");
            }
            fis.close();
            fileSender.close();
            socket.close();

            System.out.println();
            System.out.println("----- completed -----");

        } catch (IOException e) {
            closeResources();
        }
    }

    public void receiveFile(String fileName) {

        int readBytes;
        byte[] buffer = new byte[64*1024];

        try {
            requestChatService("#GET", fileName, null);

            fileName = "2" + fileName;

            Socket socket = new Socket(serverIP, subPort);
            BufferedInputStream fileReceiver = new BufferedInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(fileName);

            System.out.println("----- downloading the file -----");
            while((readBytes = fileReceiver.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, readBytes);
                fos.flush();
                if(readBytes == 64*1024)
                    System.out.print("#");
            }
            fos.close();
            fileReceiver.close();
            socket.close();

            System.out.println();
            System.out.println("----- completed -----");

        } catch(IOException e) {
            closeResources();
        }
    }

    public void closeResources() {
        try {

            if(mainSocket != null)
                mainSocket.close();
            if(msgReader != null)
                msgReader.close();
            if(msgWriter != null)
                msgWriter.close();

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

        label:
        while(true) {

            String str = scan.nextLine();

            if(str == null || str.isEmpty())
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

                        if(client != null) {
                            System.out.println("You are already in a chatroom.");
                            continue;
                        }

                        String chatRoomName = strArray[1];
                        String userName = strArray[2];

                        client = new Client(serverIP, mainPort, subPort);
                        client.requestChatService(strArray[0], chatRoomName, userName);

                        String respondMsg = client.msgReader.readLine();

                        if(respondMsg.equals("#SUCCESS")) {
                            if(strArray[0].equals("#CREATE"))
                                System.out.println("\"" + chatRoomName + "\"" + " chatroom has created!");
                            System.out.println("You are now connected to \"" + chatRoomName + "\" chatroom as " + userName);

                            client.listenForMessage();
                        }
                        else if(respondMsg.equals("#FAIL")) {
                            if(strArray[0].equals("#CREATE"))
                                System.out.println("\"" + chatRoomName + "\"" + " chatroom already exists.");
                            else
                                System.out.println("\"" + chatRoomName + "\"" + " chatroom does not exist.");

                            client.closeResources();
                            client = null;
                        }
                        else {
                            System.out.println("Unrecognized respond message.");
                            client.closeResources();
                            client = null;
                        }
                        break;
                    case "#PUT":
                    case "#GET":
                        if (strArray.length != 2) {
                            System.out.println("You have to enter (file name).");
                            continue;
                        }

                        String fileName = strArray[1];

                        if(client != null) {
                            if(strArray[0].equals("#PUT"))
                                client.sendFile(fileName);
                            else
                                client.receiveFile(fileName);
                        }
                        else
                            System.out.println("You have to join a chatroom first.");
                        break;
                    case "#EXIT":
                        if (client != null) {
                            client.requestChatService(strArray[0], null, null);
                            client.closeResources();
                            client = null;
                            System.out.println("--- Exit the chatroom ---");
                        }
                        else
                            System.out.println("You are not in a chatroom.");
                        break;
                    case "#STATUS":
                        if (client != null)
                            client.requestChatService(strArray[0], null, null);
                        else
                            System.out.println("You have to join a chatroom first.");
                        break;
                    case "#CLOSE":
                        if(client != null) {
                            client.requestChatService(strArray[0], null, null);
                            client.closeResources();
                        }
                        break label;
                    default:
                        System.out.println("Unrecognized command.");
                        break;
                }
            }
            // message From client
            else {
                if(client != null) {
                    client.writeToBuffer(str);
                }
            }
        }
    }
}
