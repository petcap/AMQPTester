public class ABoolean extends AMQPNativeType {

  boolean value;

  //Constructor
  ABoolean(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 1) throw new InvalidTypeException("Invalid type length");
    this.type = AMQPNativeType.Type.BOOLEAN;
    this.buffer = byteArrayBuffer.pop(1);
    value = (buffer.toInt() != 0); //C-style values, 0=false, else=true

  }

  public boolean toBool() {
    return value;
  }
};
