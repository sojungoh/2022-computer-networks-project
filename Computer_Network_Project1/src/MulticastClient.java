import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
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
    private DatagramChannel channel;
    private MembershipKey memKey;

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

            DatagramChannel datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET);
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            MembershipKey membershipKey = datagramChannel.join(inetAddress, networkInterface);

            this.channel = datagramChannel;
            this.memKey = membershipKey;

            //SocketAddress group = new InetSocketAddress(ipAddress, port);
            InetAddress mcastaddr = InetAddress.getByName(ipAddress);
            InetSocketAddress group = new InetSocketAddress(mcastaddr, port);

            NetworkInterface netIf = NetworkInterface.getByName("bge0");
            socket.joinGroup(group, netIf);
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

        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        channel.receive(byteBuffer);
        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes, 0, byteBuffer.limit());
    }

    public void leaveChatRoom() throws IOException {
        InetAddress mcastaddr = InetAddress.getByName(chatRoomIp);
        InetSocketAddress group = new InetSocketAddress(mcastaddr, port);
        //String myIp = getMyIPAddress();
        NetworkInterface netIf = NetworkInterface.getByName("lo");
        //socket.setNetworkInterface(netIf);
        socket.leaveGroup(group, netIf);
        memKey.drop();
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
