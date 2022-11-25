import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionReceiver extends Thread {
  private ServerSocket socket;
  private ArrayList<ClientConnection> clients;

  private ClientCodes clientCodes;

  public ConnectionReceiver(String port, ArrayList<ClientConnection> clients) throws Exception{
    if(port == null) throw new Exception("ConnectionReceiver: no port for access");
    try {
      this.socket = new ServerSocket(Integer.parseInt(port));
    } catch (Exception e) {
      throw new Exception("ConnectionReceiver: no port");
    }
    if(clients == null) {
      throw new Exception("ConnectionReceiver: no clients");
    }
    this.clients = clients;
    this.clientCodes = new ClientCodes();
  }

  public void run() {
    for(;;) {
      Socket connection = null;
      try {
        connection = this.socket.accept();
        String clientIp = connection.getInetAddress().toString();
        String clientPort = Integer.toString(connection.getPort());
        System.out.println("Connected: " + clientIp+ ":"+clientPort);
      } catch (Exception e) {
        continue;
      }
      ConnectionHandler connectionHandler = null;
      try {
        connectionHandler = new ConnectionHandler(connection, clients, clientCodes);
      } catch (Exception e) {};
      connectionHandler.start();
    }
  }
}
