/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPHeaderFrame extends AMQPInnerFrame {
  //Header class ID
  public AShortUInt amqpClass;

  //Header weight, should always be zeroed out according to the spec
  public AShortUInt weight;

  //Body size, i.e. length of body frames to follow, 64 bits
  //Zeroed out if no body frames are to follow
  public ALongLongUInt bodySize;

  //Header frame properties
  //TODO: If the LSB is set, there exists at least one more property field
  //This can be as many property fields as needed
  //In reality, only 14 flags are defined by the Basic class and hence
  //one single AShortUInt should always be enough
  public AShortUInt flags;

  //All properties within the header frame
  //This gets populated based on which flags are set in the above AShortUInt
  LinkedHashMap<AShortString, AMQPNativeType> properties = new LinkedHashMap<AShortString, AMQPNativeType>();

  //Constructor for programmatically creating new frames
  //AMQPMethodFrame() {
  //  //TODO
  //}

  //Constructor for creating frame from wire
  AMQPHeaderFrame(AShortUInt amqpClass, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {
    //Provided class
    this.amqpClass = amqpClass;

    //Pop the weight (this MUST be zeroed out according to the spec)
    this.weight = new AShortUInt(buffer);

    //Pop the body size, 64 bits long
    this.bodySize = new ALongLongUInt(buffer);

    //Pop the property flags
    //TODO: If LSB = 1, then we should read more flags
    //I don't think more than one flag field is used in any implementation
    this.flags = new AShortUInt(buffer);

    //Read all set flag values into the properties...
    //TODO: Make sure this works properly
    if (flags.getFlag(0)) properties.put(new AShortString("content-type"), new AShortString(buffer));
    if (flags.getFlag(1)) properties.put(new AShortString("content-encoding"), new AShortString(buffer));
    if (flags.getFlag(2)) properties.put(new AShortString("headers"), new AFieldTable(buffer)); //Fixme: Correct type?
    if (flags.getFlag(3)) properties.put(new AShortString("delivery-mode"), new AOctet(buffer));
    if (flags.getFlag(4)) properties.put(new AShortString("priority"), new AOctet(buffer));
    if (flags.getFlag(5)) properties.put(new AShortString("correlation-id"), new AShortString(buffer));
    if (flags.getFlag(6)) properties.put(new AShortString("reply-to"), new AShortString(buffer));
    if (flags.getFlag(7)) properties.put(new AShortString("expiration"), new AShortString(buffer));
    if (flags.getFlag(8)) properties.put(new AShortString("message-id"), new AShortString(buffer));
    if (flags.getFlag(9)) properties.put(new AShortString("timestamp"), new ALongLongUInt(buffer));
    if (flags.getFlag(10)) properties.put(new AShortString("type"), new AShortString(buffer));
    if (flags.getFlag(11)) properties.put(new AShortString("user-id"), new AShortString(buffer));
    if (flags.getFlag(12)) properties.put(new AShortString("app-id"), new AShortString(buffer));
    if (flags.getFlag(13)) properties.put(new AShortString("reserved"), new AShortString(buffer));


  }

  //For debugging
  public String toString() {
    String ret = "Header frame, class: " + amqpClass.toInt() + "\n";
    ret += " * Weight: " + weight.toString() + "\n";
    ret += " * Body size: " + bodySize.toString() + "\n";
    ret += " * Flags bit mask: 0b" + flags.toFlagString() + "\n";

    for(AShortString key : properties.keySet()) {
      ret += " * " + key.toString();

      //We do not want to print field tables recursively
      if (properties.get(key).type != AMQPNativeType.Type.FIELD_TABLE) {
        ret += " -> " + properties.get(key).toString() + "\n";
      } else {
        ret += " -> (Not printing recursively)\n";
      }
    }
    return ret;
  }

  //Get a single argument from the Method frame
  //Returns null of no such argument exists
  public AMQPNativeType getArg(String name) {
    for(AShortString key : properties.keySet()) {
      if (key.equals(new AShortString(name))) {
        return properties.get(key);
      }
    }
    System.err.println("WARNING: Returning null on header frame getArg() name=" + name);
    return null;
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    //To be populated with the frame
    ByteArrayBuffer ret = new ByteArrayBuffer();
    //TODO: Implement this
    return ret;
  }
};
