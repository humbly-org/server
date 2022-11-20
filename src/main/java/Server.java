
import java.util.ArrayList;

public class Server {
  public static String PORT = "3322";
  public static void main (String[] args) {
    if(args.length > 1) {
      System.err.println("This server that not should be use like that");
      return;
    }
    String customPort = Server.PORT;
    if (args.length == 1) customPort = args[0];

    ArrayList<ClientConnection> clients = new ArrayList<>();
    ConnectionReceiver connectionReceiver = null;
    try {
      connectionReceiver = new ConnectionReceiver(customPort, clients);
      connectionReceiver.start();
    } catch (Exception e) {
      System.err.println("Chose the right port for use!");
      return;
    }

    for(;;) {
      System.out.println("Server up and running... type: close, to close the server! ");
      System.out.println(">");
      String input = null;
      try {
        input = Keyboard.getUmString();
      } catch( Exception e) {}

      if (input.toLowerCase().equals("close")) {
        synchronized (clients) {
          for(ClientConnection client:clients) {
            try {
              client.sendMessage("sever closed");
              client.disconnect();
            } catch (Exception e) {}
          }
        }
        System.out.println("Sever closed :C\n");
        System.exit(0);
      }
      else System.err.println("Wrong input!\n");
    }
  }

}