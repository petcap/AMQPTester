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

    //System.out.println("Creating new AMQPMethodFrame, class: " + amqpClass.toString() + ", method: " + amqpMethod.toString());
    //System.out.println("Arglist is:");
    //System.out.println(buffer.toHexString());

    //Depending on which class and method, read different values:
    //Class: Connection
    if (amqpClass.toInt() == 10) {
      //Method: Start-OK
      if (amqpMethod.toInt() == 11) {
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

      //Method: Open
      if (amqpMethod.toInt() == 40) {
        arguments.put(new AShortString("path"), new AShortString(buffer));
        arguments.put(new AShortString("reserved-1"), new AOctet(buffer));
        arguments.put(new AShortString("reserved-2"), new AOctet(buffer));
      }

      //Method: Close
      if (amqpMethod.toInt() == 50) {
        arguments.put(new AShortString("reply-code"), new AShortUInt(buffer));
        arguments.put(new AShortString("reply-text"), new AShortString(buffer));
        arguments.put(new AShortString("class-id"), new AShortUInt(buffer));
        arguments.put(new AShortString("method-id"), new AShortUInt(buffer));
      }
    }

    //Class: Connection
    if (amqpClass.toInt() == 20) {
      //Method: Open
      if (amqpMethod.toInt() == 10) {
        arguments.put(new AShortString("reserved-1"), new AOctet(buffer));
      }

      //Method: Close
      if (amqpMethod.toInt() == 40) {
        arguments.put(new AShortString("reply-code"), new AShortUInt(buffer));
        arguments.put(new AShortString("reply-text"), new AShortString(buffer));
        arguments.put(new AShortString("class-id"), new AShortUInt(buffer));
        arguments.put(new AShortString("method-id"), new AShortUInt(buffer));
      }
    }

    //Class: Queue
    if (amqpClass.toInt() == 50) {
      //Method: Declare
      if (amqpMethod.toInt() == 10) {
        arguments.put(new AShortString("reserved-1"), new AShortUInt(buffer));
        arguments.put(new AShortString("queue-name"), new AShortString(buffer));
        arguments.put(new AShortString("setting-bits"), new AOctet(buffer));
        arguments.put(new AShortString("arguments"), new AFieldTable(buffer));
      }
    }

    //Class: Basic
    if (amqpClass.toInt() == 60) {
      //Method: Consume
      if (amqpMethod.toInt() == 20) {
        arguments.put(new AShortString("reserved-1"), new AShortUInt(buffer)); //Always zero, depricated value (was ticket)
        arguments.put(new AShortString("queue"), new AShortString(buffer));
        arguments.put(new AShortString("consumer-tag"), new AShortString(buffer));
        arguments.put(new AShortString("setting-bits"), new AOctet(buffer));
        arguments.put(new AShortString("arguments"), new AFieldTable(buffer));
      }

      //Method: Publish
      if (amqpMethod.toInt() == 40) {
        arguments.put(new AShortString("reserved-1"), new AShortUInt(buffer)); //Always zero, depricated value (was ticket)
        arguments.put(new AShortString("exchange-name"), new AShortString(buffer)); //Empty = default exchange
        arguments.put(new AShortString("routing-key"), new AShortString(buffer));
        arguments.put(new AShortString("mandatory"), new AOctet(buffer)); //LSB: Mandatory, Bit2: Immediate
      }
    }
  }

  //For debugging
  public String toString() {
    String ret = "Method frame: " + amqpClass.toInt() + "/" + amqpMethod.toInt() + ", arguments:\n";
    for(AShortString key : arguments.keySet()) {
      ret += " * " + key.toString();

      //We do not want to print field tables recursively
      if (arguments.get(key).type != AMQPNativeType.Type.FIELD_TABLE) {
        ret += " -> " + arguments.get(key).toString() + "\n";
      } else {
        ret += " -> (Not printing recursively)\n";
      }
    }
    return ret;
  }

  //Get a single argument from the Method frame
  //Returns null of no such argument exists
  public AMQPNativeType getArg(String name) {
    for(AShortString key : arguments.keySet()) {
      if (key.equals(new AShortString(name))) {
        return arguments.get(key);
      }
    }
    System.err.println("WARNING: Returning null on getArg() name=" + name);
    return null;
  }

  //Build an empty (i.e. with no arguments) frame
  public static AMQPFrame build(AShortUInt aclass, AShortUInt amethod) {
    return build(aclass, amethod, new LinkedHashMap<AShortString, AMQPNativeType>());
  }

  //Build an empty (i.e. with no arguments) frame
  public static AMQPFrame build(int aclass, int amethod) {
    return build(aclass, amethod, new LinkedHashMap<AShortString, AMQPNativeType>());
  }

  //Programmatically build a complete method frame in one go
  public static AMQPFrame build(AShortUInt aclass, AShortUInt amethod, LinkedHashMap<AShortString, AMQPNativeType> args) {

    //Build the inner frame
    AMQPMethodFrame method_frame = new AMQPMethodFrame(aclass, amethod, args);

    //Build the complete frame
    return new AMQPFrame(AMQPFrame.AMQPFrameType.METHOD, new AShortUInt(0), method_frame);
  }

  //Programmatically build a complete method frame in one go
  public static AMQPFrame build(AShortUInt channel, AShortUInt aclass, AShortUInt amethod, LinkedHashMap<AShortString, AMQPNativeType> args) {
    //Build the inner frame
    AMQPMethodFrame method_frame = new AMQPMethodFrame(aclass, amethod, args);

    //Build outer frame
    AMQPFrame frame = new AMQPFrame(AMQPFrame.AMQPFrameType.METHOD, new AShortUInt(0), method_frame);

    //Set channel
    frame.channel = channel;

    return frame;
  }

  //Programmatically build a complete method frame in one go
  public static AMQPFrame build(int aclass, int amethod, LinkedHashMap<AShortString, AMQPNativeType> args) {
    return build(new AShortUInt(aclass), new AShortUInt(amethod), args);
  }

  //Build one frame with one argument
  public static AMQPFrame build(int aclass, int amethod, AMQPNativeType arg) {
    LinkedHashMap<AShortString, AMQPNativeType> args = new LinkedHashMap<AShortString, AMQPNativeType>();
    args.put(new AShortString("single-argument"), arg);
    return build(new AShortUInt(aclass), new AShortUInt(amethod), args);
  }

  //Build one frame with one argument and a channel
  public static AMQPFrame build(int channel, int aclass, int amethod, AMQPNativeType arg) {
    LinkedHashMap<AShortString, AMQPNativeType> args = new LinkedHashMap<AShortString, AMQPNativeType>();
    args.put(new AShortString("single-argument"), arg);
    AMQPFrame frame = build(new AShortUInt(channel), new AShortUInt(aclass), new AShortUInt(amethod), args);
    return frame;
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
