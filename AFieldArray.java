import java.util.*;

//Field array type
public class AFieldArray extends AMQPNativeType {

  /*
  A Field Array really does not have a good definition within the AMQP 0-9-1 spec
  According to the errata, RabbitMQ interprets it like this:

  Long_UINT (4 bytes) = Length of array ()

  For each member in the array:
  - AOctet Value-Type (One char specifying the type, see spec for full list)
  - Actual value itself

  For example, an array containing a Boolean and a Short-String would be encoded like this:
  - 0x09 't' 0x01 's' 0x05 'hello'
  where
  - 0x09 = length of all the above
  - 't' = Boolean type
  - 0x01 = True
  - 's' = Short-String type
  - 'hello' = Content of string
  */

  //LinkedHashMap of all data stored within the Field Array
  LinkedList<AMQPNativeType> members = new LinkedList<AMQPNativeType>();

  //Constructor when not created from an incoming data buffer
  AFieldArray(LinkedList<AMQPNativeType> members) {
    this.members = members;
  }

  //Constructor
  //Takes the ByteArrayBuffer and pops one complete Field Array from it
  AFieldArray(ByteArrayBuffer byteArrayBuffer) throws InvalidTypeException {
    if (byteArrayBuffer.length() < 4) throw new InvalidTypeException("Invalid Field Array length: Needs 4 byte, got less");
    this.type = AMQPNativeType.Type.FIELD_ARRAY;

    //Pop first 4 bytes of the buffer into an ALongUInt
    ALongUInt length = new ALongUInt(byteArrayBuffer);

    //System.out.println("Creating new Field Array with byte size: " + length.toInt());

    //Pop the full array payload
    ByteArrayBuffer payload = byteArrayBuffer.pop(length.toLong());

    //Loop over the array and each value one by one in the member set
    while (payload.length() > 0) {
      //System.out.println("FieldArray: Length left: " + payload.length());

      //Pop the value type
      AOctet valueType = new AOctet(payload);
      //System.out.println("Value type : " + valueType.toString());

      //Now we know the key and the value type, and we need to create different
      //AMQPNativeType objects depending on what the valueType is

      //long-string
      if (valueType.toWire().toString().equals("S")) {
        ALongString value = new ALongString(payload);
        //System.out.println("Long String: " + value.toString());
        members.add(value);
        continue;
      }

      //short-string
      if (valueType.toWire().toString().equals("s")) {
        AShortString value = new AShortString(payload);
        //System.out.println("Long String: " + value.toString());
        members.add(value);
        continue;
      }

      //array-table
      if (valueType.toWire().toString().equals("F")) {
        //System.out.println("Building nested array table from:");
        //System.out.println(payload.toHexString());
        AFieldArray value = new AFieldArray(payload);
        //System.out.println("Ending nested array table");
        members.add(value);
        continue;
      }

      //boolean
      if (valueType.toWire().toString().equals("t")) {
        ABoolean value = new ABoolean(payload);
        //System.out.println("Boolean: " + (value.toBool() ? "true" : "false"));
        members.add(value);
        continue;
      }

      //If we reach down here, we were unable to understand the value type and we need to stop
      throw new InvalidTypeException("Unknown Field Array type: " + valueType.toString());
    }
  }

  //Number of members in this array table
  public int length() {
    return members.size();
  }

  //Encode data for being sent over the network
  public ByteArrayBuffer toWire() {
    //Return data, to be populated
    ByteArrayBuffer ret = new ByteArrayBuffer();

    //This will be populated with all data in the Field Array
    ByteArrayBuffer payload = new ByteArrayBuffer();

    //Loop over our members
    for(AMQPNativeType val : members) {
      try {
        //Put the value type, 1 octet length (for exampel S, s, F, t etc)
        payload.put(val.getFieldTableType());
      } catch (InvalidTypeException e) {
        //FIXME: This should never happen as the AFieldArray should never allow
        //an unsupported type to be encoded in it
        System.err.println("Unable to encode a data type to wire frame format: " + e.toString());
      }

      //Put the actual payload
      payload.put(val.toWire());
    }

    //Length header for the Field Array, 4 octets
    ByteArrayBuffer length = (new ALongUInt(payload.length())).toWire();

    //System.out.println("Field table length: " + length.toHexString());

    //Put payload length as 4 octets
    ret.put(length);

    //Add actual payload
    ret.put(payload);

    return ret;
  }

  //For debugging, return all arrays
  public String toString() {
    //StringBuilder is not needed for academic code
    String ret = "Debug: Printing Field-Array values:\n";
    ret += "----- BEGIN ARRAY -----\n";
    for(AMQPNativeType member : members) {
      ret += "Field type: " + member.getClass().getSimpleName() + "\n";
      ret += "Field valu: " + member.toString() + "\n";
      ret += "----- NEXT VALUE -----\n";
    }
    ret += "----- END ARRAY -----\n";
    return ret;
  }
};
