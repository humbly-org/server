import org.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patient implements Serializable {
  private State state;
  private int order;
  private String name;
  private String cpf;
  private LocalDateTime timeJoined;

  @Override
  public String toString() {
    return "Pessoa{" +
      "\"name\"=\"" + name + '\"' +
      ", \"cpf\"=\"" + cpf + '\"' +
      ", \"timeJoined\"=\"" + timeJoined + "\"" +
      '}';
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();
    json.put("order", this.order);
    json.put("name", this.name);
    json.put("cpf", this.cpf);
    json.put("timeJoined", this.timeJoined.toString());
    json.put("state", this.state.getActualState());
    return json;
  }

  public Patient(String name, String cpf, int order) throws Exception{
    if(!validName(name)) throw new Exception("Invalid name!");
    this.name = name;
    CpfValidator cpfValidator = new CpfValidator();
    if(!cpfValidator.isCPF(cpf)) throw new Exception("Invalid CPF!");
    this.cpf = new CpfValidator().removeSpecialChars(cpf);
    this.order = order;
    this.state = new State();
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
    this.state.setActualState(newState);
  }

}
