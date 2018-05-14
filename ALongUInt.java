//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class ALongUInt extends AMQPNativeType {

  //Constructor
  ALongUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() != 4) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.LONG_UINT;
    this.buffer = byteArrayBuffer.copy();
  }
};
