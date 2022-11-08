import java.io.IOException;
import java.net.*;
import java.util.Stack;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MulticastHandler implements Runnable {

    private int port;
    private String userName;
    private MulticastSocket socket;
    private Stack<String> chatRoomIp;

    public MulticastHandler(String port_arg) throws IOException {
       port = Integer.parseInt(port_arg);
       chatRoomIp = new Stack<>();
       socket = new MulticastSocket(port);
    }

    public String getUserName() {
        return this.userName;
    }

    public void joinChatRoom(String chatRoomName, String userName) throws IOException {

        this.userName = userName;
        String ipAddress = getIpByChatRoomName(chatRoomName);

        if(ipAddress != null) {
            chatRoomIp.push(ipAddress);
            InetAddress mcastaddr = InetAddress.getByName(ipAddress);
            InetSocketAddress group = new InetSocketAddress(mcastaddr, port);
            NetworkInterface netIf = NetworkInterface.getByName("bge0");
            socket.joinGroup(group, netIf);
        }
    }

    public void receiveMessage() throws IOException {

        byte[] chunk = new byte[512];

        while(true) {
            DatagramPacket packet = new DatagramPacket(chunk, chunk.length);

            socket.receive(packet);

            String message = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println(message);

            if ("OK".equals(message)) break;
        }
    }

    public void sendMessage(String message, String userName)
            throws IOException {

        String ipAddress = chatRoomIp.peek();
        InetAddress group = InetAddress.getByName(ipAddress);
        String msgWithName = userName + ": " + message;

        byte[] msg = msgWithName.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
        socket.send(packet);
    }

    public void leaveChatRoom() throws IOException {
        String currRoomIp = chatRoomIp.peek();

        InetAddress mcastaddr = InetAddress.getByName(currRoomIp);
        InetSocketAddress group = new InetSocketAddress(mcastaddr, port);
        NetworkInterface netIf = NetworkInterface.getByName("bge0");
        socket.leaveGroup(group, netIf);

        chatRoomIp.pop();

        if(chatRoomIp.empty())
            socket.close();
    }

    public String getIpByChatRoomName(String chatRoomName) {

        String ipAddress;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(chatRoomName.getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();

            ipAddress = "225.";
            int x = Byte.toUnsignedInt(byteData[byteData.length - 3]);
            int y = Byte.toUnsignedInt(byteData[byteData.length - 2]);
            int z = Byte.toUnsignedInt(byteData[byteData.length - 1]);

            ipAddress += Integer.toString(x) + "." + Integer.toString(y) + "." + Integer.toString(z);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ipAddress = null;
        }

        return ipAddress;
    }

    @Override
    public void run() {
        try {

            receiveMessage();

        } catch(IOException e){
            socket.close();
        }
    }
}
