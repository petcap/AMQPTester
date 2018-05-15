//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AShortUInt extends AMQPNativeType {

  //Constructor
  AShortUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 2) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.SHORT_UINT;
    this.buffer = byteArrayBuffer.pop(2);
  }

  public String toString() {
    return "" + buffer.toInt();
  }

  public int toInt() {
    return buffer.toInt();
  }
};
