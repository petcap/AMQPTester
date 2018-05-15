/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPMethodFrame extends AMQPInnerFrame {
  //Class and method represented by this frame
  public AShortUInt amqpClass;
  public AShortUInt amqpMethod;

  //LinkedHashMap of all mathod arguments
  LinkedHashMap<String, AMQPNativeType> arguments = new LinkedHashMap<String, AMQPNativeType>();

  //Constructor
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
        System.out.println(buffer.toHexString());

        //Read Client-Properties as a FieldTable
        //Read one field table (Client-properties)
        arguments.put("Client-Properties", new AFieldTable(buffer));

        //Re-encode the frame and see what we get
        //FIXME: This is not correct
        System.out.println(arguments.get("Client-Properties").toWire().toHexString());

        //Debug print
        //System.out.println(arguments.get("Client-Properties").toString());
      }
    }
  }
}
