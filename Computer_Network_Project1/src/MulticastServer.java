import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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

        InetAddress group = InetAddress.getByName(ipAddress);

        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
        socket.send(packet);
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
