//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AShortString extends AMQPNativeType {

  public AOctet length;

  //Constructor
  AShortString(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    this.type = AMQPNativeType.Type.SHORT_STRING;
    this.length = new AOctet(byteArrayBuffer);

    if (length.toInt() > byteArrayBuffer.length()) {
      throw new InvalidTypeException("Specified string length is longer than existing buffer");
    }

    this.buffer = byteArrayBuffer.pop(length.toInt());
  }

  public String toString() {
    return buffer.toString();
  }
};
