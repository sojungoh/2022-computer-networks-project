import java.io.IOException;
import java.util.Scanner;
import java.net.*;

public class Peer {

    public static void main(String[] args) throws IOException {

        Thread t1, t2;
        MulticastSocket socket = null;

        if(args.length != 1){
            System.out.println("wrong port number");
            System.exit(0);
        }

        int port = Integer.parseInt(args[0]);
        socket = new MulticastSocket(port);
        MulticastClient client = new MulticastClient(port, socket);

        Scanner scanner = new Scanner(System.in);
        boolean isChatting = false;

        while(true) {

            String str = scanner.nextLine();

            Boolean isCommand = (str.charAt(0) == '#');

            if(isCommand){

                String[] strArray = str.split("\\s");

                if(strArray[0].equals("#JOIN")) {

                    if(isChatting) {
                        System.out.println("You first have to leave this chat room");
                        continue;
                    }

                    isChatting = true;

                    String chatRoomName = strArray[1];
                    String userName = strArray[2];

                    client.joinChatRoom(chatRoomName, userName);

                    t1 = new Thread(client);
                    t1.start();

                    System.out.println("-- Enter the chatroom " + chatRoomName + " --");
                }
                else if(strArray[0].equals("#EXIT")) {

                    if(!isChatting) {
                        System.out.println("You're not in a chat room");
                        continue;
                    }

                    isChatting = false;

                    client.leaveChatRoom();

                    System.out.println("-- Exit the chatroom --");

                    socket.close();
                    break;
                }
                else
                    System.out.println("Unrecognized command");
            }
            else {
                String msgWithName = client.getUserName() + ": " + str;
                MulticastServer server =
                        new MulticastServer(socket, msgWithName,
                                client.getPort(), client.getIpAddress());
                t2 = new Thread(server);
                t2.start();
            }
        }
    }
}
