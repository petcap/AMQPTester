//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class ALongUInt extends AMQPNativeType {

  long value;

  //Constructor
  ALongUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 4) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.LONG_UINT;
    this.buffer = byteArrayBuffer.pop(4);

    //Convert data from wire to value
    this.value = ByteArrayBuffer.bytesToLong(this.buffer.get());
  }

  ALongUInt(long value) {
    this.type = AMQPNativeType.Type.LONG_UINT;
    this.value = value;
  }

  public long toLong() {
    return value;
  }

  public String toString() {
    return "" + value;
  }

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    //A long is 8 bytes, cut the 4 MSB
    ByteArrayBuffer ret = new ByteArrayBuffer(ByteArrayBuffer.longToBytes(value));
    ret.pop(4);
    return ret;
  }

  public int toInt() {
    return (int) value;
  }
};
