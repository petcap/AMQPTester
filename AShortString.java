//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AShortString extends AMQPNativeType {

  //Constructor
  AShortString(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    this.type = AMQPNativeType.Type.SHORT_STRING;
    AOctet length = new AOctet(byteArrayBuffer);

    if (length.toInt() > byteArrayBuffer.length()) {
      throw new InvalidTypeException("Specified short string length is longer than existing buffer");
    }

    this.buffer = byteArrayBuffer.pop(length.toInt());
  }

  //Hack just to get another constructor from which we can create short strings
  //internally, since we want to be able to create a string directly from a
  //ByteArrayBuffer without specifying the length
  AShortString(int length, ByteArrayBuffer byteArrayBuffer) {
    this.type = AMQPNativeType.Type.SHORT_STRING;
    this.buffer = byteArrayBuffer;
  }

  //Constructor from string
  AShortString(String value) {
    this.type = AMQPNativeType.Type.SHORT_STRING;
    this.buffer = new ByteArrayBuffer(value);
  }

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    AOctet len = new AOctet(buffer.length());

    //Return length (1 octet) + the string itself
    ByteArrayBuffer ret = len.toWire();
    ret.put(buffer);
    return ret;
  }

  public String toString() {
    return buffer.toString();
  }
};
