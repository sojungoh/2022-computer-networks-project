import java.io.IOException;
import java.net.*;
import static java.lang.Thread.sleep;

public class MulticastServer implements Runnable {

    private int port;
    private String ipAddress;
    private String message;
    private DatagramSocket socket;

    public MulticastServer(String msg, int port, String ip) throws IOException {
        this.port = port;
        this.ipAddress = ip;
        this.message = msg;
        this.socket = new DatagramSocket();
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
            sleep(5000);
        } catch (IOException | InterruptedException e) {
            socket.close();
            //e.printStackTrace();
        }

    }
}
