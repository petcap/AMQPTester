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

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    //Length of the string
    ALongUInt len = new ALongUInt(buffer.length());

    //Return length (4 octet) + the string itself
    ByteArrayBuffer ret = len.toWire();
    ret.put(buffer);
    return ret;
  }

  //Constructor from string
  ALongString(String value) {
    this.type = AMQPNativeType.Type.LONG_STRING;
    this.buffer = new ByteArrayBuffer(value);
  }

  public String toString() {
    return buffer.toString();
  }
};
