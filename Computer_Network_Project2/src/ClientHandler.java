import java.util.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private static HashMap<String, ArrayList<ClientHandler>> chatRoomMap = new HashMap<>();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String userName;
    private String currChatRoomName;

    public ClientHandler(Socket socket) throws IOException {
        try {

            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.userName = reader.readLine();
            this.currChatRoomName = null;

        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    public Boolean isChatRoomExist(String chatRoomName) {
        return chatRoomMap.containsKey(chatRoomName);
    }

    public void createChatRoom(String chatRoomName) {

        try {
            if(currChatRoomName != null) {
                writer.write("You are already in a chatroom.");
                writer.newLine();
                writer.flush();
                return;
            }

            if(!isChatRoomExist(chatRoomName)) {

                this.currChatRoomName = chatRoomName;
                chatRoomMap.put(chatRoomName, new ArrayList<ClientHandler>());
                chatRoomMap.get(chatRoomName).add(this);

                writer.write("\"" + chatRoomName + "\"" + " chatroom has created!");
                writer.newLine();
                writer.flush();

                writer.write("You are now connected to \"" + chatRoomName + "\" chatroom as " + userName);
            }
            else
                writer.write("\"" + chatRoomName + "\"" + " chatroom already exists.");

            writer.newLine();
            writer.flush();

        } catch(IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    public void joinChatRoom(String chatRoomName) {
        try {
            if(currChatRoomName != null) {
                writer.write("You are already in a chatroom.");
                writer.newLine();
                writer.flush();
                return;
            }

            if(isChatRoomExist(chatRoomName)) {
                this.currChatRoomName = chatRoomName;
                chatRoomMap.get(chatRoomName).add(this);
                writer.write("You are now connected to \"" + chatRoomName + "\" chatroom as " + userName);
            }
            else
                writer.write("\"" + chatRoomName + "\"" + " chatroom does not exist.");

            writer.newLine();
            writer.flush();

        } catch(IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    public void broadcastMessage(String message) {

        String chatFormat;
        ArrayList<ClientHandler> clientBuckets;

        if(this.currChatRoomName == null)
            return;

        if(!isChatRoomExist(this.currChatRoomName))
            return;

        clientBuckets = chatRoomMap.get(this.currChatRoomName);

        for(ClientHandler client : clientBuckets) {
            try {
                if(client != this) {
                    chatFormat = "FROM " + this.userName + ": " + message;
                    client.writer.write(chatFormat);
                    client.writer.newLine();
                    client.writer.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, reader, writer);
            }
        }
    }

    public void removeClientHandler() {

        ArrayList<ClientHandler> clientBuckets;

        if(this.currChatRoomName == null)
            return;

        if(!isChatRoomExist(this.currChatRoomName))
            return;

        clientBuckets = chatRoomMap.get(this.currChatRoomName);
        clientBuckets.remove(this);
    }

    public void sendChatRoomInfo() {

        ArrayList<ClientHandler> clientBuckets;
        int order = 1;

        try {
            if(this.currChatRoomName == null || !isChatRoomExist(this.currChatRoomName)) {
                writer.write("No chatroom status");
                writer.newLine();
                writer.flush();
                return;
            }

            clientBuckets = chatRoomMap.get(this.currChatRoomName);

            writer.write("Chatroom name: " + this.currChatRoomName);
            writer.newLine();
            writer.flush();

            writer.write("----- member list -----");
            writer.newLine();
            writer.flush();

            for(ClientHandler client : clientBuckets) {
                writer.write(order + ". " + client.userName);
                writer.newLine();
                writer.flush();
                order += 1;
            }

        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }

    }

    public void closeEverything(Socket socket, BufferedReader reader, BufferedWriter writer) {
        removeClientHandler();
        try {
            if(reader != null)
                reader.close();
            if(writer != null)
                writer.close();
            if(socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        String clientMsg = null;
        String chatRoomName = null;

        try {

            while(!socket.isClosed()) {

                clientMsg = reader.readLine();

                if(clientMsg == null || clientMsg.isEmpty())
                    continue;

                if(clientMsg.equals("#CREATE")) {
                    chatRoomName = reader.readLine();
                    createChatRoom(chatRoomName);
                }
                else if(clientMsg.equals("#JOIN")) {
                    chatRoomName = reader.readLine();
                    joinChatRoom(chatRoomName);
                }
                else if(clientMsg.equals("#EXIT")) {
                    closeEverything(socket, reader, writer);
                }
                else if(clientMsg.equals("#STATUS")) {
                    sendChatRoomInfo();
                }
                else {
                    broadcastMessage(clientMsg);
                }
            }

        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }
}
