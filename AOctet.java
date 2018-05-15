//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AOctet extends AMQPNativeType {

  int value;

  //Constructor
  AOctet(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 1) throw new InvalidTypeException("AOctet too short input");
    this.type = AMQPNativeType.Type.OCTET;
    this.value = byteArrayBuffer.pop(1).toInt();
  }

  //Constructor
  AOctet(int value) {
    this.type = AMQPNativeType.Type.OCTET;
    this.value = value;
  }

  public int toInt() {
    return value;
  }

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    //A long is 8 bytes, cut the 7 MSB to get the octet
    ByteArrayBuffer ret = new ByteArrayBuffer(ByteArrayBuffer.longToBytes(value));
    ret.pop(7);
    return ret;
  }

  public String toString() {
    return "" + value;
  }
};
