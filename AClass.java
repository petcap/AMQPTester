//Enumerator for the AMQP Class types
//TODO: Delete this? Not used?
public enum AClass {
  CONNECTION((int) 10),
  CHANNEL((int) 20),
  EXCHANGE((int) 40),
  QUEUE((int) 50),
  BASIC((int) 60),
  TX((int) 90);

  private int frameType;

  AClass(int frameType) {
    this.frameType = frameType;
  }

  public int get() {
    return frameType;
  }
};
