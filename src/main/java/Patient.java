import org.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patient implements Serializable {
  private String state;
  private int order;
  private String name;
  private String cpf;
  private String severity = "low";
  private LocalDateTime timeJoined;

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();
    json.put("id", this.cpf);
    json.put("name", this.name);
    json.put("cpf", this.cpf);
    json.put("startAt", this.timeJoined.toString());
    json.put("queueType", this.state);
    json.put("severity", this.severity);
    json.put("queuePosition", this.order);
    return json;
  }

  public Patient(String name, Object cpf, int order, String state) throws Exception{
    if(!validName(name)) throw new Exception("Invalid name!");
    this.name = name;
    CpfValidator cpfValidator = new CpfValidator();
    if(!cpfValidator.isCPF(cpf)) throw new Exception("Invalid CPF!");
    this.cpf = new CpfValidator().removeSpecialChars(cpf).toString();
    this.order = order;
    this.state = state;
    this.timeJoined = LocalDateTime.now();
  }

  private boolean validName(String name){
    String expression = "^[a-zA-Z]{4,}(?: [a-zA-Z]+){0,5}$";
    Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(name);
    return matcher.matches();
  }


  public String getCpf() {
    return this.cpf;
  }

  public void changeState(String newState) {
    this.state = newState;
  }

}
