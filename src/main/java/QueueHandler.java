import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueueHandler{
  private String queueType;

  ArrayList<Patient> queue;

  public QueueHandler(String queueType) throws Exception {
    if (queueType == null) throw new Exception("Null queueType");
    this.queueType = queueType;
    this.queue = new ArrayList<Patient>();
  }

  public ArrayList<Patient> getQueue() {
    return this.queue;
  }

  public void addOnQueue(Patient patient) throws Exception {
    if (patient == null) throw new Exception("Null patient!");
    this.queue.add(patient);
  }

  public Patient changeQueue(String patientCpf, QueueHandler newQueue)
    throws Exception {
    if (patientCpf == null) throw new Exception("Null patient cpf!");
    List<Patient> arr = filter((p -> Objects.equals(p.getCpf(), patientCpf)),
      this.queue);
    Patient patient = arr.get(0);
    this.queue.removeIf(p -> p.getCpf() == patientCpf);
    patient.changeState(newQueue.getQueueType());
    newQueue.addOnQueue(patient);
    return patient;
  }

  public JSONArray getQueueJSON() {
    JSONArray jsonArr = new JSONArray();
    queue.forEach(patient -> {
      jsonArr.put(patient.toJsonObject());
    });
    return jsonArr;
  }

  public <T> boolean containsPatient(T cpf) {
    String patientCpf = cpf.toString();
    List<Patient> arr = filter(
      p -> p.getCpf().equals(new CpfValidator().removeSpecialChars(patientCpf)),
      this.queue);
    return arr.size() >= 1;
  }

  public Patient getPatient(String patientCpf) {
    List<Patient> arr = filter(
      p -> p.getCpf().equals(new CpfValidator().removeSpecialChars(patientCpf)),
      this.queue);
    return arr.get(0);
  }

  public String getQueueType() {
    return this.queueType;
  }

  public <T> List<T> filter(Predicate<T> criteria, ArrayList<T> list) {
    return list.stream().filter(criteria).collect(Collectors.<T>toList());
  }

  public int getOrder() {
    return this.queue.size();
  }
}
