import java.io.IOException;
import java.util.Scanner;

public class Peer {

    public static void main(String[] args) throws IOException {

        if(args.length != 1){
            System.out.println("wrong port number");
            System.exit(0);
        }

        String port_arg = args[0];
        MulticastHandler peer = new MulticastHandler(port_arg);

        Thread t = new Thread(peer);
        t.start();

        int joinCount = 0;
        Scanner scanner = new Scanner(System.in);

        while(true) {

            String str = scanner.nextLine();

            Boolean isCommand = (str.charAt(0) == '#');

            if(isCommand){

                String[] strArray = str.split("\\s");

                if(strArray[0].equals("#JOIN")) {
                    String chatRoomName = strArray[1];
                    String userName = strArray[2];
                    peer.joinChatRoom(chatRoomName, userName);
                    joinCount += 1;

                    System.out.println("-- You enter the chatroom " + chatRoomName + " --");
                }
                else if(strArray[0].equals("#EXIT")) {
                    peer.leaveChatRoom();
                    joinCount -= 1;

                    System.out.println("-- You exit the chatroom --");

                    if (joinCount == 0) {
                        break;
                        // server close;
                    }
                }
                else
                    System.out.println("Unrecognized command");

            }
            else {
                if(joinCount == 0) {
                    System.out.println("You're not in any chat room");
                    continue;
                }
                peer.sendMessage(str, peer.getUserName());
            }
        }
    }
}
