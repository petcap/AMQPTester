//Class represeting a type in AMQP, such as long-string, short-string, long, long-long etc
//Inherited by other subclasses
public class AMQPNativeType {

  //(Some) native AMQP types
  public enum Type {
    OCTET, //1 byte
    SHORT_UINT, //2 bytes
    LONG_UINT, //4 bytes
    LONGLONG_UINT, //8 bytes
    SHORT_STRING, //OCTET + String
    LONG_STRING, // LONG_UINT (4 bytes) + String
    TIMESTAMP, //8 bytes, unix TS
    FIELD_TABLE, //LONG_UINT (4 bytes) + Table data
    FIELD_ARRAY, //LONG_UINT (4 bytes) + Array data
    FIELD_VALUE_PAIR, //SHORT_STRING + OCTET type + Field data
    BOOLEAN, //1 byte, 0x00 = false, otherwise = true
    SHORTSHORT_INT, //1 byte
    SHORTSHORT_UINT, //1 byte
  }

  //Raw data
  public ByteArrayBuffer buffer;

  public Type type;

  //Constructor
  //AMQPNativeType() throws InvalidTypeException {}

  //Encode data type to wire
  public ByteArrayBuffer toWire() {
    return new ByteArrayBuffer();
  }

  //Returns a ByteArrayBuffer of the expected field type,
  //i.e S for Long String and s for Short String
  //Throws InvalidTypeException if cannot be included in a field table
  public ByteArrayBuffer getFieldTableType() throws InvalidTypeException {
    if (type == Type.LONG_STRING) {
      return new ByteArrayBuffer((byte) 'S');
    }

    if (type == Type.BOOLEAN) {
      return new ByteArrayBuffer((byte) 't');
    }

    if (type == Type.FIELD_TABLE) {
      return new ByteArrayBuffer((byte) 'F');
    }

    if (type == Type.SHORT_STRING) {
      return new ByteArrayBuffer((byte) 's');
    }

    if (type == Type.FIELD_ARRAY) {
      return new ByteArrayBuffer((byte) 'A');
    }

    if (type == Type.SHORT_UINT) {
      return new ByteArrayBuffer((byte) 'u');
    }

    if (type == Type.LONG_UINT) {
      return new ByteArrayBuffer((byte) 'i');
    }

    throw new InvalidTypeException("Cannot encode native type to Field Table: " + type.name());
  }
};
