public class ABoolean extends AMQPNativeType {

  boolean value;

  //Constructor
  ABoolean(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 1) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.BOOLEAN;
    this.buffer = byteArrayBuffer.pop(1);
    value = (buffer.toInt() != 0); //C-style values, 0=false, else=true

  }

  ABoolean(boolean value) {
    this.type = AMQPNativeType.Type.BOOLEAN;
    this.value = value;
  }

  //Get boolean value
  public boolean toBool() {
    return value;
  }

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    if (value) return new ByteArrayBuffer(new byte[]{0x01}); //True
    return new ByteArrayBuffer(new byte[]{0x00}); //False
  }

  public String toString() {
    return value ? "True" : "False";
  }
};
