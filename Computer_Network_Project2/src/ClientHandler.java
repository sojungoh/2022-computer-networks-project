import java.util.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    public static HashMap<String, ArrayList<ClientHandler>> chatRoomMap = new HashMap<>();
    private Socket mainSocket;
    private ServerSocket subSocket;
    private BufferedReader msgReader;
    private BufferedWriter msgWriter;
    private String currChatRoomName;
    private String currUserName;

    public ClientHandler(Socket mainSocket, ServerSocket subSocket) throws IOException {
        try {

            this.mainSocket = mainSocket;
            this.subSocket = subSocket;
            this.msgReader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
            this.msgWriter = new BufferedWriter(new OutputStreamWriter(mainSocket.getOutputStream()));
            this.currChatRoomName = null;
            this.currUserName = null;

        } catch (IOException e) {
            closeResources();
        }
    }

    public void writeToBuffer(String msg) {
        try {

            msgWriter.write(msg);
            msgWriter.newLine();
            msgWriter.flush();

        } catch(IOException e) {
            closeResources();
        }
    }

    public Boolean isChatRoomExist(String chatRoomName) {
        return chatRoomMap.containsKey(chatRoomName);
    }

    public void createChatRoom(String chatRoomName, String userName) {

        if(!isChatRoomExist(chatRoomName)) {

            this.currChatRoomName = chatRoomName;
            this.currUserName = userName;

            chatRoomMap.put(chatRoomName, new ArrayList<>());
            chatRoomMap.get(chatRoomName).add(this);
            writeToBuffer("#SUCCESS");
        }
        else
            writeToBuffer("#FAIL");

    }

    public void joinChatRoom(String chatRoomName, String userName) {

        if(isChatRoomExist(chatRoomName)) {

            this.currChatRoomName = chatRoomName;
            this.currUserName = userName;

            chatRoomMap.get(chatRoomName).add(this);
            writeToBuffer("#SUCCESS");
        }
        else
            writeToBuffer("#FAIL");
    }

    public void broadcastMessage(String message) {

        String chatFormat;
        ArrayList<ClientHandler> clientBuckets;

        if(!isChatRoomExist(this.currChatRoomName))
            return;

        clientBuckets = chatRoomMap.get(this.currChatRoomName);

        for(ClientHandler client : clientBuckets) {
            if(client != this) {
                chatFormat = "FROM " + this.currUserName + ": " + message;
                client.writeToBuffer(chatFormat);
            }
        }
    }

    public void removeClientHandler() {

        ArrayList<ClientHandler> clientBuckets;

        if(!isChatRoomExist(this.currChatRoomName))
            return;

        clientBuckets = chatRoomMap.get(this.currChatRoomName);
        clientBuckets.remove(this);

        if(clientBuckets.isEmpty()) {
            chatRoomMap.remove(this.currChatRoomName);
        }

        this.currChatRoomName = null;
        this.currUserName = null;
    }

    public void sendChatRoomInfo() {

        int order = 1;
        ArrayList<ClientHandler> clientBuckets;

        if(!isChatRoomExist(this.currChatRoomName)) {
            writeToBuffer("No chatroom status");
            return;
        }

        clientBuckets = chatRoomMap.get(this.currChatRoomName);

        writeToBuffer("Chatroom name: " + this.currChatRoomName);
        writeToBuffer("----- member list -----");

        for(ClientHandler client : clientBuckets) {
            writeToBuffer(order + ". " + client.currUserName);
            order += 1;
        }
    }

    public void receiveFile(String fileName) {

        int readBytes;
        byte[] buffer = new byte[64*1024];

        try {
            fileName = "1" + fileName;

            Socket socket = subSocket.accept();
            BufferedInputStream fileReceiver = new BufferedInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(fileName);

            while((readBytes = fileReceiver.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, readBytes);
                fos.flush();
            }
            fos.close();
            fileReceiver.close();

            String msg = "Get a file => " + fileName;
            broadcastMessage(msg);

        } catch(IOException e) {
            closeResources();
        }
    }

    public void sendFile(String fileName) {

        int readBytes;
        byte[] buffer = new byte[64*1024];  // 64KiB
        File file = new File(fileName);

        if(!file.exists()) {
            writeToBuffer("File name \"" + fileName + "\" does not exist.");
            return;
        }

        try {
            Socket socket = subSocket.accept();
            BufferedOutputStream fileSender = new BufferedOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);

            while((readBytes = fis.read(buffer)) > 0) {
                fileSender.write(buffer, 0, readBytes);
                fileSender.flush();
            }
            fis.close();
            fileSender.close();

        } catch(IOException e) {
            closeResources();
        }
    }

    public void closeResources() {

        removeClientHandler();
        try {

            if(mainSocket != null)
                mainSocket.close();
            if(subSocket != null)
                subSocket.close();
            if(msgReader != null)
                msgReader.close();
            if(msgWriter != null)
                msgWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        String clientMsg;
        String chatRoomName;
        String userName;
        String fileName;

        try {

            while(!mainSocket.isClosed()) {

                System.out.println("waiting");
                clientMsg = msgReader.readLine();
                System.out.println("read");

                if(clientMsg == null || clientMsg.isEmpty())
                    continue;

                switch (clientMsg) {
                    case "#CREATE":
                        chatRoomName = msgReader.readLine();
                        userName = msgReader.readLine();
                        createChatRoom(chatRoomName, userName);
                        break;
                    case "#JOIN":
                        chatRoomName = msgReader.readLine();
                        userName = msgReader.readLine();
                        joinChatRoom(chatRoomName, userName);
                        break;
                    case "#PUT":
                        fileName = msgReader.readLine();
                        receiveFile(fileName);
                        break;
                    case "#GET":
                        fileName = msgReader.readLine();
                        sendFile(fileName);
                        break;
                    case "#EXIT":
                        removeClientHandler();
                        break;
                    case "#STATUS":
                        sendChatRoomInfo();
                        break;
                    case "#CLOSE":
                        closeResources();
                        break;
                    default:
                        broadcastMessage(clientMsg);
                        break;
                }
            }

            System.out.println("Client disconnected from the server.");

        } catch (IOException e) {
            closeResources();
        }
    }
}
