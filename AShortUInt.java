//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AShortUInt extends AMQPNativeType {

  long value;

  //Constructor
  AShortUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 2) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.LONG_UINT;
    this.buffer = byteArrayBuffer.pop(2);

    //Convert data from wire to value
    this.value = ByteArrayBuffer.bytesToLong(this.buffer.get());
  }

  AShortUInt(long value) {
    this.type = AMQPNativeType.Type.SHORT_UINT;
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
    //A long is 8 bytes, cut the 6 MSB
    ByteArrayBuffer ret = new ByteArrayBuffer(ByteArrayBuffer.longToBytes(value));
    ret.pop(6);
    return ret;
  }

  public int toInt() {
    return (int) value;
  }
};
