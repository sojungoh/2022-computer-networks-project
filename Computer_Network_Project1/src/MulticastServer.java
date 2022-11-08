import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class MulticastServer implements Runnable {

    private int port;
    private String ipAddress;
    private String message;
    private MulticastSocket socket;

    public MulticastServer(MulticastSocket socket, String msg, int port, String ip) throws IOException {
        this.port = port;
        this.ipAddress = ip;
        this.message = msg;
        this.socket = socket;
    }

    public void sendMessage() throws IOException {

        //InetAddress group = InetAddress.getByName(ipAddress);

        DatagramChannel datagramChannel=DatagramChannel.open();
        datagramChannel.bind(null);
        NetworkInterface networkInterface=NetworkInterface.getByName("bge0");
        datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF,networkInterface);
        ByteBuffer byteBuffer=ByteBuffer.wrap(message.getBytes());
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, port);
        datagramChannel.send(byteBuffer,inetSocketAddress);


        //InetAddress mcastaddr = InetAddress.getByName(ipAddress);
        //InetSocketAddress group = new InetSocketAddress(mcastaddr, port);

        //byte[] msg = message.getBytes();
        //DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
    }

    @Override
    public void run() {

        try {

            sendMessage();

        } catch (IOException e) {
            socket.close();
            //e.printStackTrace();
        }

    }
}
