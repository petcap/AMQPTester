//Long long unsigned int
//Does not actually hold 64 (unsigned) bits, so this is limited to 63 bits precision
//Could be resolved by using BigInteger
public class ALongLongUInt extends AMQPNativeType {

  long value;

  //Constructor
  ALongLongUInt(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 8) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.LONGLONG_UINT;
    this.buffer = byteArrayBuffer.pop(8);

    //Convert data from wire to value
    this.value = ByteArrayBuffer.bytesToLong(this.buffer.get());
  }

  ALongLongUInt(long value) {
    this.type = AMQPNativeType.Type.LONGLONG_UINT;
    this.value = value;
  }

  ALongLongUInt(int value) {
    this.type = AMQPNativeType.Type.LONGLONG_UINT;
    this.value = (long) value;
  }

  public long toLong() {
    return value;
  }

  public String toString() {
    return "" + value;
  }

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    return new ByteArrayBuffer(ByteArrayBuffer.longToBytes(value));
  }

  public int toInt() {
    return (int) value;
  }
};
