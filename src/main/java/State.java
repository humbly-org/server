public class State {
  private String actualState;

  public String getActualState() {
    return actualState;
  }

  public void setActualState(String actualState) {
    this.actualState = actualState;
  }

  public State() {
    this.actualState = "onHold";
  }

}
