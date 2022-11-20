import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionHandler extends Thread{
  private double value = 0;
  private ClientConnection client;
  private Socket connection;
  private ArrayList<ClientConnection> clients;

  public ConnectionHandler(Socket connection ,ArrayList<ClientConnection> clients) throws Exception {
    if(connection == null) throw new Exception("No socket in ConnectionHandler");
    if(clients == null) throw new Exception("No clients in ConectionHandler");
    this.connection = connection;
    this.clients = clients;
  }

  public void run() {
    ObjectOutputStream writer = null;
    try {
      writer = new ObjectOutputStream(this.connection.getOutputStream());
    } catch (Exception e) {return;}
    BufferedReader logger = null;
    try {
      logger = new BufferedReader (new InputStreamReader(connection.getInputStream ()));
    } catch (Exception e) {
      try {
        writer.close();
      } catch (Exception error) {}
      return;
    }

    try {
      this.client = new ClientConnection(this.connection, logger, writer);
    } catch (Exception e) {}
    try {
      synchronized (this.clients) { this.clients.add(this.client);}
      for (;;) {
        String text = client.readMessage();
        if(text == null) return;
        else {
          this.sendMessageToAll(text);
        }
      }
    } catch (Exception e) {
      try {
        writer.close();
        logger.close();
      } catch (Exception error) {}
      return;
    }
  }

  public <T> void sendMessageToAll(T text) throws Exception {
    if(text == null) return;
    synchronized (clients) {
      try {
        for(ClientConnection client:clients) {
          client.sendMessage(text);
        }
      } catch (Exception e) {
        System.out.println("sendMessageToAll: erro");
      }
    }
  }
}
