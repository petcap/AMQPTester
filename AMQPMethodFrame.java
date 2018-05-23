/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPMethodFrame extends AMQPInnerFrame {
  //Class and method represented by this frame
  public AShortUInt amqpClass;
  public AShortUInt amqpMethod;

  //LinkedHashMap of all mathod arguments
  LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();

  //Constructor for programmatically creating new frames
  AMQPMethodFrame(AShortUInt amqpClass, AShortUInt amqpMethod, LinkedHashMap<AShortString, AMQPNativeType> arguments) {

    //Assign given class and method
    this.amqpClass = amqpClass;
    this.amqpMethod = amqpMethod;
    this.arguments = arguments;
  }

  //Constructor for creating from buffers
  AMQPMethodFrame(AShortUInt amqpClass, AShortUInt amqpMethod, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {

    //Assign given class and method
    this.amqpClass = amqpClass;
    this.amqpMethod = amqpMethod;

    System.out.println("Creating new AMQPMethodFrame, class: " + amqpClass.toString() + ", method: " + amqpMethod.toString());
    //System.out.println("Arglist is:");
    //System.out.println(buffer.toHexString());

    //Depending on which class and method, read different values:
    //Class: Connection
    if (amqpClass.toInt() == 10) {
      //Method: Start-OK
      if (amqpMethod.toInt() == 11) {
        //Print current buffer...
        //System.out.println(buffer.toHexString());

        //Read the specific arguments to this method and store them in the hashmap
        //They are stored in order in the linked hashmap which is crucial for encoding
        //frames later
        arguments.put(new AShortString("client-properties"), new AFieldTable(buffer));
        arguments.put(new AShortString("mechanism"), new AShortString(buffer));
        arguments.put(new AShortString("response"), new ALongString(buffer));
        arguments.put(new AShortString("locale"), new AShortString(buffer));
      }
      //Method: Tune-OK
      if (amqpMethod.toInt() == 31) {
        arguments.put(new AShortString("channel-max"), new AShortUInt(buffer));
        arguments.put(new AShortString("frame-max"), new ALongUInt(buffer));
        arguments.put(new AShortString("heartbeat"), new AShortUInt(buffer));
      }
    }
  }

  //For debugging
  public String toString() {
    String ret = "(Method frame) class/method: " + amqpClass.toInt() + "/" + amqpMethod.toInt() + ", arguments:";
    for(AShortString key : arguments.keySet()) {
      ret += " " + key.toString();

      //We do not want to print field tables recursively
      if (arguments.get(key).type != AMQPNativeType.Type.FIELD_TABLE) {
        ret += "(" + arguments.get(key).toString() + ")";
      }
    }
    return ret;
  }

  //Programmatically build a complete method frame in one go
  public static AMQPFrame build(AShortUInt aclass, AShortUInt amethod, LinkedHashMap<AShortString, AMQPNativeType> args) {

    //Build the inner frame
    AMQPMethodFrame method_frame = new AMQPMethodFrame(aclass, amethod, args);

    //Build the complete frame
    return new AMQPFrame(AMQPFrame.AMQPFrameType.METHOD, new AShortUInt(0), method_frame);
  }

  //Programmatically build a complete method frame in one go
  public static AMQPFrame build(int aclass, int amethod, LinkedHashMap<AShortString, AMQPNativeType> args) {
    return build(new AShortUInt(aclass), new AShortUInt(amethod), args);
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    //To be populated with the frame
    ByteArrayBuffer ret = new ByteArrayBuffer();

    //Method frames are pretty simple:
    //Class + Method + Argument struct
    //Argument struct needs to be in order, but since the LinkedHashMap is in order
    //it is possible to just encode stuff directly from it

    //Put the class and method names first
    ret.put(amqpClass.toWire());
    ret.put(amqpMethod.toWire());

    //Put the argument list
    for(AMQPNativeType val : arguments.values()) {
      ret.put(val.toWire());
    }

    return ret;
  }
};
