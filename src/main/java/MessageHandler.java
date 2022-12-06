import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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

  public ClientConnection getPatientConnection(String cpf, ArrayList<ClientConnection> clients) {
    ArrayList<ClientConnection> patientClient = new ArrayList<>(clients);
    patientClient.removeIf(p -> !p.getId().equals(cpf));
    return patientClient.get(0);
  }


  public <T> void sendMessageToHospitals(T text,
                                        ArrayList<ClientConnection> clients)
    throws Exception {
    if (text == null || clients.size() == 0) return;
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


  public <T> void sendMessageToPatients(T text,
                                     ArrayList<ClientConnection> clients)
    throws Exception {
    if (text == null || clients.size() == 0) return;
    ArrayList<ClientConnection> patients =
      (ArrayList<ClientConnection>) getPatients(clients);
    synchronized (patients) {
      try {
        for (ClientConnection client : patients) {
          client.sendMessage(text);
        }
      }
      catch (Exception e) {
        System.out.println("sendMessageToAll: erro");
      }
    }
  }

  public <T> void sendMessageToPatientsOnHold(T text,
                                        ArrayList<ClientConnection> clients)
    throws Exception {
    if (text == null || clients.size() == 0) return;
    ArrayList<ClientConnection> patients =
      (ArrayList<ClientConnection>) getPatients(clients);
    synchronized (patients) {
      try {
        for (ClientConnection client : patients) {
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
        long averageTimeInMinutes = this.averageTimeToCallOnMinutes();
        String res = this.generateMessage("enterQueueRes",
          new JSONObject().put("message", "Added on Queue").put(
            "averageTimeInMinutes", averageTimeInMinutes));
        connection.sendMessage(res);
        String hospitalMessage = this.generateMessage("newPatient",
          patient.toJsonObject());
        this.sendMessageToHospitals(hospitalMessage, clients);
      }
    }
    catch (Exception e) {
      throw new Exception(e.getMessage());
    }

  }

  public ArrayList<Patient> getAllPatients()
  throws Exception{
    try {
      ArrayList<Patient> onHoldQueuePatients = onHoldQueue.getQueue();
      ArrayList<Patient> inProgressQueuePatients = inProgressQueue.getQueue();
      ArrayList<Patient> finishedQueuePatients = finishedQueue.getQueue();
      ArrayList<Patient> allPatients = new ArrayList<Patient>();
      for (int i = 0; i < onHoldQueuePatients.size(); i++) {
        allPatients.add(onHoldQueuePatients.get(i));
      }
      for (int i = 0; i < inProgressQueuePatients.size(); i++) {
        allPatients.add(inProgressQueuePatients.get(i));
      }
      for (int i = 0; i < finishedQueuePatients.size(); i++) {
        allPatients.add(finishedQueuePatients.get(i));
      }
      return allPatients;
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  public ArrayList<Patient> getAtendedPatients()
    throws Exception{
    try {
      ArrayList<Patient> allPatients = this.getAllPatients();
      allPatients.removeIf(p -> !(boolean)p.isAtended());
      return allPatients;
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }


  public long averageTimeToCallOnMinutes()
    throws Exception{
    try {
      ArrayList<Patient> atendedPatients = this.getAtendedPatients();
      AtomicLong averageTime = new AtomicLong();
      atendedPatients.forEach(patient -> {
        averageTime.addAndGet(patient.timeToCallIMinutes());
      });
      long averageTimeToCallOnMinutesLong = averageTime.get();
      if(atendedPatients.size() == 0) {
        return averageTimeToCallOnMinutesLong/1;
      }
      return averageTimeToCallOnMinutesLong/atendedPatients.size();
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }


  public void hospitalLogin(ClientConnection connection)
    throws Exception {
    try {
      connection.setId("HOSPITAL");
      JSONArray onHoldQueueJSON = this.onHoldQueue.getQueueJSON();
      JSONArray inProgressQueueJSON = this.inProgressQueue.getQueueJSON();
      JSONArray finishedQueueJSON = this.finishedQueue.getQueueJSON();
      JSONArray allJSON =
        mergeMultiJsonArray(onHoldQueueJSON, inProgressQueueJSON, finishedQueueJSON);

      String res = this.generateMessage("hospitalLoginRes",
        new JSONObject().put("patients", allJSON));
      connection.sendMessage(res);

    }
    catch (Exception e) {
      throw new Exception(e.getMessage());
    }
  }

  public static JSONArray mergeMultiJsonArray(JSONArray... arrays) {
    JSONArray outArray = new JSONArray();
    for (JSONArray array : arrays)
      for (int i = 0; i < array.length(); i++)
        outArray.put(array.optJSONObject(i));
    return outArray;
  }

  public void changeQueue(String body, ClientConnection connection,
                          ArrayList<ClientConnection> clients) throws Exception {
    JSONObject patientObject = new JSONObject(body);
    try {
      String patientCpf = (String) patientObject.get("patientCpf");
      String nextQueue = (String) patientObject.get("nextQueue");
      String requestedOrigin = (String) patientObject.get("requestedOrigin");
      if (inProgressQueue.containsPatient(patientCpf) && nextQueue.equals("finished")
       ) {
        Patient updatedPatient = this.inProgressQueue.changeQueue(patientCpf,
          this.finishedQueue);
        ClientConnection patientConection =
          this.getPatientConnection(patientCpf, clients);
        patientConection.changeState("finished");
        if(requestedOrigin.equals("PATIENT")) updatedPatient.patientFinishedByItself();
        JSONObject data = new JSONObject();
        data.put("state", "finished");
        String res = generateMessage("changeQueueRes", data);
        ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
        patient.sendMessage(res);
        res = generateMessage("updatedPatient", updatedPatient.toJsonObject());
        this.sendMessageToHospitals(res, clients);
      } else if (onHoldQueue.containsPatient(patientCpf) && nextQueue.equals(
        "finished")
      ) {
        Patient updatedPatient = this.onHoldQueue.changeQueue(patientCpf,
          this.finishedQueue);
        ClientConnection patientConection =
          this.getPatientConnection(patientCpf, clients);
        patientConection.changeState("finished");
        if(requestedOrigin.equals("PATIENT")) updatedPatient.patientFinishedByItself();
        JSONObject data = new JSONObject();
        data.put("state", "finished");
        String res = generateMessage("changeQueueRes", data);
        ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
        patient.sendMessage(res);
        res = generateMessage("updatedPatient", updatedPatient.toJsonObject());
        this.sendMessageToHospitals(res, clients);
      } else {
        Patient updatedPatient = this.onHoldQueue.changeQueue(patientCpf,
          this.inProgressQueue);
        ClientConnection patientConection =
          this.getPatientConnection(patientCpf, clients);
        patientConection.changeState("onHold");
        if(requestedOrigin.equals("HOSPITAL")) updatedPatient.patientAtendetAt();
        JSONObject data = new JSONObject();
        data.put("state", "inProgress");
        String res = generateMessage("changeQueueRes", data);
        ClientConnection patient = this.getConnectionCpf(patientCpf, clients);
        patient.sendMessage(res);
        res = generateMessage("updatedPatient", updatedPatient.toJsonObject());
        this.sendMessageToHospitals(res, clients);
        if(requestedOrigin.equals("HOSPITAL")) {
          long newAverageTimeToCall = this.averageTimeToCallOnMinutes();
          JSONObject message = new JSONObject().put("message",
            "timeToCallUpdated").put("body", new JSONObject().put(
            "averageTimeToCall", newAverageTimeToCall));
          this.sendMessageToPatientsOnHold(message.toString(), clients);
        }
      }
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

  private List<ClientConnection> getHospitals(ArrayList<ClientConnection> clients) throws Exception {
    List<ClientConnection> hospitals = filter(c -> c.getId().equals("HOSPITAL"),
      clients);
    if (hospitals.size() > 0) return hospitals;
    throw new Exception("Sem hospitais conectados!");
  }

  private List<ClientConnection> getPatients(ArrayList<ClientConnection> clients) throws Exception {
    List<ClientConnection> patients = filter(c -> !(c.getId().equals("HOSPITAL")),
      clients);
    if (patients.size() > 0) return patients;
    throw new Exception("Sem clientes conectados!");
  }

  private List<ClientConnection> getPatientOnHold(ArrayList<ClientConnection> clients) throws Exception {
    List<ClientConnection> patients = filter(c -> !(c.getId().equals(
      "HOSPITAL") && c.getState().equals("onHold")),
      clients);
    if (patients.size() > 0) return patients;
    throw new Exception("Sem clientes conectados!");
  }


  public <T> List<T> filter(Predicate<T> criteria, ArrayList<T> list) {
    return list.stream().filter(criteria).collect(Collectors.<T>toList());
  }
}
