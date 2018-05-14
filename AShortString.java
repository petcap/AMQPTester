//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AShortString extends AMQPNativeType {

  //Constructor
  AShortString(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    this.type = AMQPNativeType.Type.SHORT_STRING;
    this.buffer = byteArrayBuffer.copy();
  }
};
