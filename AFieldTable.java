import java.util.*;

//Field table type
public class AFieldTable extends AMQPNativeType {

  /*
  A Field Table looks like this:
  Long_UINT (4 bytes) = Length of table payload

  For each member in the field:
  ShortString Key
  AOctet Value-Type (Basically one char specifying the type, see spec for full list)
  Variable Value
  */

  //HashMap of all data stored within the Field Table
  HashMap<AShortString, AMQPNativeType> members = new HashMap<AShortString, AMQPNativeType>();

  //Constructor
  //Takes the ByteArrayBuffer and pops one complete Field Table from it
  AFieldTable(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 4) throw new InvalidTypeException("Invalid Field Table length");
    this.type = AMQPNativeType.Type.FIELD_TABLE;

    //Pop first 4 bytes of the buffer into an ALongUInt
    ALongUInt length = new ALongUInt(byteArrayBuffer);

    //Pop the field table payload
    ByteArrayBuffer payload = byteArrayBuffer.pop(length.toLong());

    //Loop over the fields and store them one by one in the HashMap
    while (payload.length() > 0) {
      //System.out.println("FieldTable: Length left: " + payload.length());

      System.out.println("----------------------------");

      //Pop the key string from the buffer
      AShortString key = new AShortString(payload);
      System.out.println("Key        : " + key.toString());

      //Pop the value type
      AOctet valueType = new AOctet(payload);
      System.out.println("Value type : " + valueType.toString());

      //Now we know the key and the value type, and we need to create different
      //AMQPNativeType objects depending on what the valueType is
      AMQPNativeType value = null;

      //long-string
      if (valueType.toString().equals("S")) {
        value = new ALongString(payload);
        System.out.println("Long String: " + value.toString());
      }

      System.out.println("----------------------------");

      //Make sure we understand the data types we're getting
      //Unfortunately, we cannot simply ignore some types as they might have
      //arbitrary lengths, causing us to lose track of where we should continue
      //reading data
      if (value == null) {
        throw new InvalidTypeException("Unknown Field Table type: " + valueType.toString());
      }

      //Store argument in HashMap
      members.put(key, value);

    }
  }

  //Number of members in this field table
  public int length() {
    return members.size();
  }
};
