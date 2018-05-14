import java.util.*;

//Field table type
public class AFieldTable extends AMQPNativeType {

  /*
  A Field Table looks like this:
  Long_UINT (4 bytes) = Length of table payload

  For each member in the field:
  ShortString Key
  Variable Value
  */

  //HashMap of all data stored within the Field Table
  HashMap<AShortString, AMQPNativeType> arguments = new HashMap<AShortString, AMQPNativeType>();

  ALongUInt length;

  //Constructor
  //Takes the ByteArrayBuffer and pops one complete Field Table from it
  AFieldTable(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 4) throw new InvalidTypeException("Invalid Field Table length");
    this.type = AMQPNativeType.Type.FIELD_TABLE;

    //Pop first 4 bytes of the buffer into an ALongUInt
    length = new ALongUInt(byteArrayBuffer);

    //Pop the field table payload
    ByteArrayBuffer payload = byteArrayBuffer.pop(length.toLong());

    //Loop over the fields and store them one by one in the HashMap
    while (payload.length() > 0) {
      System.out.println("FieldTable: Length left: " + payload.length());

      //Pop the key string from the buffer
      AShortString key = new AShortString(payload);
      System.out.println("Key: " + key.toString());
    }
  }

  public ALongUInt length() {
    return length;
  }
};
