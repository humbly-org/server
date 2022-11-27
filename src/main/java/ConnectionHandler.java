import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionHandler extends Thread{
  private ClientConnection client;
  private Socket connection;
  private String id;
  private ArrayList<ClientConnection> clients;

  private MessageHandler messageHandler;

  private ClientCodes clientCodes;

  public ConnectionHandler(
    Socket connection, ArrayList<ClientConnection> clients,
    ClientCodes clientCodes, MessageHandler messageHandler
  ) throws Exception {
    if (connection == null) {
      throw new Exception("No socket in ConnectionHandler");
    }
    if (clients == null) throw new Exception("No clients in ConectionHandler");
    this.connection = connection;
    this.clients = clients;
    this.messageHandler = messageHandler;
    this.clientCodes = clientCodes;
  }

  public void run() {
    ObjectOutputStream writer = null;
    try {
      writer = new ObjectOutputStream(this.connection.getOutputStream());
    }
    catch (Exception e) {
      return;
    }
    BufferedReader logger = null;
    try {
      logger =
        new BufferedReader(new InputStreamReader(connection.getInputStream()));
    }
    catch (Exception e) {
      try {
        writer.close();
      }
      catch (Exception error) {
      }
      return;
    }

    try {
      this.client = new ClientConnection(this.connection, logger, writer, id);
    }
    catch (Exception e) {
    }
    try {
      synchronized (this.clients) {
        this.clients.add(this.client);
      }
      for (; ; ) {
        String text = client.readMessage();
        JSONObject convertedText = new JSONObject(text);
        if (text == null) {
          return;
        }
        else {
          try {
            this.messageHandler.handleMessage(convertedText, client,
              clientCodes, clients);
          }
          catch (Exception e) {
            System.out.println(e.getMessage());
            JSONObject error =
              new JSONObject("{\"message\":\"Something wrong on server\"}");
            this.sendMessageToAll(error.toString());
          }

        }
      }
    }
    catch (Exception e) {
      try {
        writer.close();
        logger.close();
      }
      catch (Exception error) {
      }
      return;
    }
  }

  public void setCpf(String id) {
    this.id = new CpfValidator().removeSpecialChars(id);
  }

  public String getCpf() {
    return this.id;
  }

  public <T> void sendMessage(T message) throws Exception {
    this.client.sendMessage(message.toString());
  }

  public <T> void sendMessageToAll(T text) throws Exception {
    if (text == null) return;
    synchronized (clients) {
      try {
        for (ClientConnection client : clients) {
          client.sendMessage(text);
        }
      }
      catch (Exception e) {
        System.out.println("sendMessageToAll: erro");
      }
    }
  }
}
