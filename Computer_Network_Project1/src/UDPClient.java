import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Scanner;

public class UDPClient {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
//        System.out.print("Enter the port number: ");
//        int portNum = sc.nextInt();
        int portNum = Integer.parseInt(args[0]);

        String chatroom;
        String nickname;
        while(true) {
            String joinCmd = bf.readLine();
            String[] token = joinCmd.split(" ");
            if(token[0].equals("#JOIN") && token.length >= 3) {
                chatroom = token[1];
                nickname = token[2];
                break;
            } else {
                System.out.println("Join the chatting room first!");
            }
        }



        MessageDigest hash = MessageDigest.getInstance("SHA-512");
        hash.update(chatroom.getBytes());
        byte byteData[] = hash.digest();
        int len = byteData.length;

        System.out.println(byteData[len-1] + " " + byteData[len-2] + " " + byteData[len-3]);

        String x,y,z;
        int tmpx, tmpy, tmpz;
        if(byteData[len-1] < 0) {
            tmpx = -byteData[len-1];
        } else {
            tmpx = byteData[len-1];
        }

        if(byteData[len-2] < 0) {
            tmpy = -byteData[len-2];
        } else {
            tmpy = byteData[len-2];
        }

        if(byteData[len-3] < 0) {
            tmpz = -byteData[len-3];
        } else {
            tmpz = byteData[len-3];
        }

        x = Integer.toString(tmpx);
        y = Integer.toString(tmpy);
        z = Integer.toString(tmpz);

        String ip = "225." + x + "." + y + "." + z;
        System.out.println(ip);

        MulticastSocket socket = new MulticastSocket(portNum);

        InetAddress ip_a = InetAddress.getByName(ip);
        socket.joinGroup(ip_a);

        String s = "----" + nickname + " entered the chat room " + chatroom + "----\n";
        byte[] buf = new byte[512];
        buf = s.getBytes();

        DatagramPacket tmpPacket = new DatagramPacket(buf, buf.length,ip_a, portNum);
        socket.send(tmpPacket);

        try { /* send Thread */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        String cmd = null;
                        try {
                            cmd = bf.readLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        byte[] chunck = new byte[512];

                        if(cmd.equals("#EXIT")) {
                            String leaveMessage = "----" + nickname + " leaved this room----\n";
                            chunck = leaveMessage.getBytes();

                            DatagramPacket packet = new DatagramPacket(chunck, chunck.length, ip_a, portNum);
                            try {
                                socket.send(packet);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                socket.leaveGroup(InetAddress.getByName(ip));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            System.exit(0);
                        } else if(cmd.length() + nickname.length() + 3 < 512) {
                            String message = nickname + " : " + cmd;
                            chunck = message.getBytes();
                            DatagramPacket packet = new DatagramPacket(chunck, chunck.length, ip_a, portNum);

                            try {
                                socket.send(packet);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            int total_len = cmd.length();
                            int cur = 0;

                            while(cur < total_len) {
                                int remain = total_len - cur;
                                int m_len;

                                if(remain < (512 - 3 - nickname.length())) {
                                    m_len = remain;
                                } else {
                                    m_len = 512 - 3 - nickname.length();
                                }
                                String message = nickname + " : " + cmd.substring(cur , cur + m_len);

                                chunck = message.getBytes();
                                DatagramPacket packet = new DatagramPacket(chunck, chunck.length, ip_a, portNum);
                                try {
                                    socket.send(packet);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }


                        }
                    }
                }
            }).start();
        } catch(Exception e) {
            e.printStackTrace();
        }

        try { /* receive Thread */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        byte[] buf = new byte[512];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);

                        try {
                            socket.receive(packet);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        String content = new String(packet.getData(), 0, packet.getLength());
                        System.out.println(content);
                    }
                }
            }).start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

