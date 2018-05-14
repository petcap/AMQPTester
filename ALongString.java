//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class ALongString extends AMQPNativeType {

  public ALongUInt length;

  //Constructor
  ALongString(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    this.type = AMQPNativeType.Type.LONG_STRING;
    this.length = new ALongUInt(byteArrayBuffer);

    if (length.toInt() > byteArrayBuffer.length()) {
      throw new InvalidTypeException("Specified long string length is longer than existing buffer");
    }

    this.buffer = byteArrayBuffer.pop(length.toInt());
  }

  public String toString() {
    return buffer.toString();
  }
};
