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

    System.out.println("Creating new Field Table with byte size: " + length.toInt());

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

      //long-string
      if (valueType.toString().equals("S")) {
        ALongString value = new ALongString(payload);
        System.out.println("Long String: " + value.toString());
        members.put(key, value);
        continue;
      }

      //field-table
      if (valueType.toString().equals("F")) {
        System.out.println("Building nested field table");
        AFieldTable value = new AFieldTable(payload);
        System.out.println("Ending nested field table");
        members.put(key, value);
        continue;
      }

      //boolean
      if (valueType.toString().equals("t")) {
        ABoolean value = new ABoolean(payload);
        System.out.println("Boolean: " + (value.toBool() ? "true" : "false"));
        members.put(key, value);
        continue;
      }

      //If we reach down here, we were unable to understand the value type and we need to stop
      throw new InvalidTypeException("Unknown Field Table type: " + valueType.toString());


    }
  }

  //Number of members in this field table
  public int length() {
    return members.size();
  }
};
