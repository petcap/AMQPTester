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
    //Make a new buffer
    ByteArrayBuffer ret = new ByteArrayBuffer(ByteArrayBuffer.longToBytes(value));

    //Cut 6 bytes
    ret.pop(6);

    //Return the remaining value
    return ret;
  }

  //Get value as a boolean array
  public String toFlagString() {
    //To be returned
    String ret = Long.toBinaryString(value);

    //Make sure we build the string so that it is exacly 16 chars long
    while (ret.length() < 16) {
      ret = "0" + ret;
    }

    return ret;
  }

  //Get value as a boolean array
  public boolean[] getFlags() {
    //Return value
    boolean[] ret = new boolean[16];

    byte[] tmp = toFlagString().getBytes();

    for(int i=0; i!=16; ++i) {
      ret[i] = (tmp[15-i] == '1');
    }

    return ret;
  }

  public boolean equals(AShortUInt other) {
    return (this.value == other.value);
  }

  //Get a specific bit mask flag
  //0 = LSB
  //15 = MSB
  public boolean getFlag(int flag) {
    return getFlags()[flag];
  }

  public int toInt() {
    return (int) value;
  }
};
