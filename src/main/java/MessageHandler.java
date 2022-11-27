import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessageHandler {
  QueueHandler onHoldQueue;
  QueueHandler inProgressQueue;
  QueueHandler finishedQueue;

  public MessageHandler() throws Exception {
    this.onHoldQueue = new QueueHandler("onHold");
    this.inProgressQueue = new QueueHandler("inProgress");
    this.finishedQueue = new QueueHandler("finished");
  }

  private String generateMessage(String message, JSONObject data) {
    JSONObject json = new JSONObject();
    json.put("message", message);
    json.put("body", data);
    return json.toString();
  }

  public void callPatient(
    String body, ClientConnection connection,
    ClientCodes clientCodes, ArrayList<ClientConnection> clients
  ) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      Object patientCpf = patientObject.get("patientCpf");
      if (onHoldQueue.containsPatient(patientCpf)) {
        String code = clientCodes.generateCode();
        JSONObject data = new JSONObject();
        data.put("patientCode", code);
        String res = generateMessage("callPatientRes", data);
        ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
        patient.sendMessage(res);
        connection.sendMessage(res);

      }
      else {
        throw new Exception("Patient are not on Hold");
      }
    }
    catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }



  public <T> void sendMessageToAll(T text, ArrayList<ClientConnection> clients)
    throws Exception {
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

  public <T> void sendMessageToHospitals(T text, ArrayList<ClientConnection> clients)
    throws Exception {
    if (text == null) return;
    ArrayList<ClientConnection> hospitals =
      (ArrayList<ClientConnection>) getHospitals(clients);
    synchronized (hospitals) {
      try {
        for (ClientConnection client : hospitals) {
          client.sendMessage(text);
        }
      }
      catch (Exception e) {
        System.out.println("sendMessageToAll: erro");
      }
    }
  }

  public void enterQueue(String body, ClientConnection connection, ArrayList<ClientConnection> clients)
    throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientName = (String) patientObject.get("patientName");
      Object patientCpf = patientObject.get("patientCpf");
      if (onHoldQueue.containsPatient(patientCpf)) {
        String res = this.generateMessage("enterQueueRes",
          new JSONObject().put("message", "Allready on Queue"));
        connection.sendMessage(res);
      }
      else {
        int order = this.onHoldQueue.getOrder();
        Patient patient = new Patient(patientName, patientCpf, order,
          this.onHoldQueue.getQueueType());
        this.onHoldQueue.addOnQueue(patient);
        connection.setId(patientCpf);
        String res = this.generateMessage("enterQueueRes",
          new JSONObject().put("message", "Added on Queue"));
        connection.sendMessage(res);
        res = this.generateMessage("newPatient", patient.toJsonObject());
        this.sendMessageToHospitals(res, clients);
      }
    }
    catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  public void hospitalLogin(ClientConnection connection)
    throws Exception {
    try {
      connection.setId("HOSPITAL");
      String res = this.generateMessage("hospitalLoginRes",
        new JSONObject().put("message", "Hospital Login"));
      connection.sendMessage(res);

    }
    catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  public void changeQueue(String body, ClientConnection connection,
                          ArrayList<ClientConnection> clients) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientCpf = (String) patientObject.get("patientCpf");
      if (inProgressQueue.containsPatient(patientCpf)) {
        throw new Exception("Patient already on queue!");
      }
      Patient updatedPatient = this.onHoldQueue.changeQueue(patientCpf,
        this.inProgressQueue);
      JSONObject data = new JSONObject();
      data.put("state", "inProgres");
      String res = generateMessage("changeQueueRes", data);
      ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
      patient.sendMessage(res);
      res = generateMessage("updatedPatient", updatedPatient.toJsonObject());
      connection.sendMessage(res);
    }
    catch (Exception e) {
      throw new Exception("Something wrong with body");
    }
  }

  public void mapper(
    String type, JSONObject rest, ClientConnection connection,
    ClientCodes clientCodes, ArrayList<ClientConnection> clients
  ) throws Exception {
    String body = "";
    try {
      body = rest.getJSONObject("body").toString();
    }
    catch (Exception e) {
    }
    Map<String, Runnable> methods = new HashMap<>();
    String finalBody = body;
    methods.put("callPatient", () -> {
      try {
        callPatient(finalBody, connection, clientCodes, clients);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("destroy", () -> {
      try {
        connection.disconnect();
        clients.removeIf(client -> client == connection);
        System.out.println("Client desconected!");
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("enterQueue", () -> {
      try {
        enterQueue(finalBody, connection, clients);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("hospitalLogin", () -> {
      try {
        hospitalLogin(connection);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("changeQueue", () -> {
      try {
        changeQueue(finalBody, connection, clients);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("sendMessageToAll", () -> {
      try {
        sendMessageToAll(finalBody, clients);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.get(type).run();
  }

  public void handleMessage(
    JSONObject message, ClientConnection connection, ClientCodes clientCodes,
    ArrayList<ClientConnection> clients
  ) throws Exception {
    String type = message.getString("message");
    mapper(type, message, connection, clientCodes, clients);
  }

  private ClientConnection getConnectionCpf(
    Object patientCpf, ArrayList<ClientConnection> clients
  ) {
    String cpf = new CpfValidator().removeSpecialChars(patientCpf);
    List<ClientConnection> client = filter(c -> c.getId().equals(cpf), clients);
    if (client.size() > 0) {
      ClientConnection patient = client.get(0);
      return patient;
    }
    return null;
  }

  private List<ClientConnection> getHospitals(ArrayList<ClientConnection> clients) {
    List<ClientConnection> hospitals = filter(c -> c.getId().equals("HOSPITAL"),
      clients);
    if (hospitals.size() > 0) return hospitals;
    return null;
  }


  public <T> List<T> filter(Predicate<T> criteria, ArrayList<T> list) {
    return list.stream().filter(criteria).collect(Collectors.<T>toList());
  }
}
