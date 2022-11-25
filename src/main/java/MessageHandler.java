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

  private String generateMessage(String message, JSONObject data){
    JSONObject json = new JSONObject();
    json.put("message", message);
    json.put("body", data);
    return json.toString();
  }

  public void callPatient(String body, ClientConnection connection, ClientCodes clientCodes, ArrayList<ClientConnection> clients) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientCpf = (String)patientObject.get("patientCpf");
      if(onHoldQueue.containsPatient(patientCpf)) {
        String code = clientCodes.generateCode();
        JSONObject data = new JSONObject();
        data.put("patientCode", code);
        String res = generateMessage("callPatientRes", data);
        ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
        patient.sendMessage(res);
        connection.sendMessage(res);

      } else {
        throw new Exception("Patient are not on Hold");
      }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  public void enterQueue(String body, ClientConnection connection) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientName = (String) patientObject.get("patientName");
      String patientCpf = (String)patientObject.get("patientCpf");
      if(onHoldQueue.containsPatient(patientCpf)) {
        String res = this.generateMessage("enterQueueRes", new JSONObject().put("message", "Allready on Queue"));
        connection.sendMessage(res);
      } else {
        int order = this.onHoldQueue.getOrder();
        this.onHoldQueue.addOnQueue(new Patient(patientName, patientCpf, order));
        connection.setCpf(patientCpf);
        String res = this.generateMessage("enterQueueRes", new JSONObject().put("message", "Added on Queue"));
        connection.sendMessage(res);
      }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  public void changeQueue(String body) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientCpf = (String)patientObject.get("patientCpf");
      if(onHoldQueue.containsPatient(patientCpf)) throw new Exception("Patient already on queue!");
      this.onHoldQueue.changeQueue(patientCpf, this.inProgressQueue);
    } catch (Exception e) {
      throw new Exception("Something wrong with body");
    }
  }

  public void mapper(String type, JSONObject rest, ClientConnection connection, ClientCodes clientCodes, ArrayList<ClientConnection> clients) throws Exception {
    String body = rest.getJSONObject("body").toString();
    Map<String, Runnable> methods = new HashMap<>();
    methods.put("callPatient", () -> {
      try {
        callPatient(body, connection, clientCodes, clients);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("enterQueue", () -> {
      try {
        enterQueue(body, connection);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.put("changeQueue", () -> {
      try {
        changeQueue(body);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    methods.get(type).run();
  }

  public void handleMessage(JSONObject message, ClientConnection connection, ClientCodes clientCodes,  ArrayList<ClientConnection> clients) throws Exception {
    String type = message.getString("message");
    mapper(type, message, connection, clientCodes, clients);
  }

  private ClientConnection getConnectionCpf(String patientCpf, ArrayList<ClientConnection> clients) {
    String cpf = new CpfValidator().removeSpecialChars(patientCpf);
    List<ClientConnection> client = filter(c -> c.getId().equals(cpf), clients);
    if(client.size() > 0) {
      ClientConnection patient = client.get(0);
      return patient;
    }
    return null;
  }

  public<T> List<T> filter(Predicate<T> criteria, ArrayList<T> list) {
    return list.stream().filter(criteria).collect(Collectors.<T>toList());
  }
}
