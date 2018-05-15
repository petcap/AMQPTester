//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class ALongLongUInt extends AMQPNativeType {

  //Constructor
  ALongLongUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 8) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.LONGLONG_UINT;
    this.buffer = byteArrayBuffer.pop(8);
  }

  public String toString() {
    return "" + buffer.toLong();
  }
};
