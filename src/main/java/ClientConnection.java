import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;

public class ClientConnection{
  private Socket connection;
  private BufferedReader logger;
  private ObjectOutputStream writer;

  private String state;

  private String id = "";

  public ClientConnection(
    Socket connection, BufferedReader logger, ObjectOutputStream writer,
    String id
  ) throws Exception {
    if (connection == null) {
      throw new Exception("Missing connection parameter!");
    }
    if (logger == null) throw new Exception("Missing logger parameter!");
    if (writer == null) throw new Exception("Missing writer parameter!");
    this.connection = connection;
    this.logger = logger;
    this.writer = writer;
  }

  public <T> void setId(T patientCpf) {
    this.id = new CpfValidator().removeSpecialChars(patientCpf.toString());
  }

  public String readMessage() throws Exception {
    StringBuffer buffer = new StringBuffer();
    int ch;
    boolean run = true;
    try {
      while (run) {
        ch = this.logger.read();
        if (ch == -1) {
          break;
        }
        buffer.append((char) ch);
        if (isJSONValid(buffer.toString())) {
          run = false;
        }
      }
    }
    catch (SocketTimeoutException e) {
    }
    return buffer.toString();
  }

  public void changeState(String state) {
    this.state = state;
  }

  public String getState() {
    return this.state;
  }

  private boolean isJSONValid(String test) {
    try {
      new JSONObject(test);
    }
    catch (JSONException ex) {
      try {
        new JSONArray(test);
      }
      catch (JSONException ex1) {
        return false;
      }
    }

    return true;
  }

  public <T> void sendMessage(T type) throws Exception {
    try {
      if (type == null || type == "") return;
      try {
        char charTest = type.toString().charAt(0);
      }
      catch (Exception e) {
        return;
      }
      System.out.println("Sended Message: " + type.toString());
      this.writer.writeObject(type);
      this.writer.flush();
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  ;

  public void disconnect() throws Exception {
    try {
      this.writer.close();
      this.logger.close();
      this.connection.close();
    }
    catch (Exception e) {
      throw new Exception("disconnect error");
    }
  }

  public String getId() {
    return this.id;
  }
}
