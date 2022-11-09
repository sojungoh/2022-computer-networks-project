import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastClient implements Runnable {

    private int port;
    private String userName;
    private MulticastSocket socket;
    private String chatRoomIp;

    public MulticastClient(int port, MulticastSocket socket) throws IOException {
        this.port = port;
        this.socket = socket;
    }

    public int getPort() {
        return this.port;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getIpAddress() {
        return this.chatRoomIp;
    }

    public void joinChatRoom(String chatRoomName, String userName) throws IOException {

        this.userName = userName;
        String ipAddress = getIpByChatRoomName(chatRoomName);

        System.out.println("ip: " + ipAddress);

        if (ipAddress != null) {
            this.chatRoomIp = ipAddress;

            InetAddress mcastaddr = InetAddress.getByName(ipAddress);
            InetSocketAddress group = new InetSocketAddress(mcastaddr, port);
            NetworkInterface netIf = NetworkInterface.getByName("eth0");

            if(netIf != null)
                socket.joinGroup(group, netIf);
            else
                socket.joinGroup(mcastaddr);
        }
    }

    public void receiveMessage() throws IOException {

        byte[] chunk = new byte[512];

        while (true) {
            DatagramPacket packet = new DatagramPacket(chunk, chunk.length);

            socket.receive(packet);

            String message = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println(message);

            if ("OK".equals(message)) break;
        }

    }

    public void leaveChatRoom() throws IOException {
        InetAddress mcastaddr = InetAddress.getByName(chatRoomIp);
        InetSocketAddress group = new InetSocketAddress(mcastaddr, port);
        NetworkInterface netIf = NetworkInterface.getByName("localhost");
        socket.leaveGroup(group, netIf);
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

        } catch (IOException e) {
            socket.close();
            //e.printStackTrace();
        }
    }
}
