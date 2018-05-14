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
    FIELD_VALUE_PAIR, //SHORT_STRING + OCTET type + Field data
    BOOLEAN, //1 byte, 0x00 = false, otherwise = true
    SHORTSHORT_INT, //1 byte
    SHORTSHORT_UINT, //1 byte
  }

  //Raw data
  public ByteArrayBuffer buffer;

  public Type type;

  //Constructor
  AMQPNativeType() {

  }
};
