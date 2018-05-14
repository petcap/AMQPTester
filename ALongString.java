//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class ALongString extends AMQPNativeType {

  //Constructor
  ALongString(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    this.type = AMQPNativeType.Type.LONG_STRING;
    this.buffer = byteArrayBuffer.copy();
  }
};
