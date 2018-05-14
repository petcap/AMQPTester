//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AOctet extends AMQPNativeType {

  //Constructor
  AOctet(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() != 1) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.OCTET;
    this.buffer = byteArrayBuffer.copy();
  }
};
