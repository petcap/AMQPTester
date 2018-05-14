/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPMethodFrame extends AMQPInnerFrame {
  //Class and method represented by this frame
  public ByteArrayBuffer amqpClass;
  public ByteArrayBuffer amqpMethod;

  //HashMap of all mathod arguments
  HashMap<String, AMQPNativeType> arguments = new HashMap<String, AMQPNativeType>();

  //Constructor
  AMQPMethodFrame(ByteArrayBuffer amqpClass, ByteArrayBuffer amqpMethod, ByteArrayBuffer buffer) throws InvalidFrameException {
    //Copy the argument list, as we will be modifying it
    buffer = buffer.copy();

    //Assign given class and method
    this.amqpClass = amqpClass;
    this.amqpMethod = amqpMethod;

    System.out.println("Creating new AMQPMethodFrame, class: " + amqpClass.toLong() + ", method: " + amqpMethod.toLong());
    System.out.println("Arglist is:");
    System.out.println(buffer.toHexString());

    //Depending on which class and method, read different values:
    //Class: Connection
    if (amqpClass.toLong() == 10) {
      //Method: Start-OK
      if (amqpMethod.toLong() == 11) {
        //Read Client-Properties as a FieldTable
        try {
          arguments.put("Client-Properties", new AFieldTable(buffer));
        } catch (InvalidTypeException e) { //Failed to decode argument data?
          throw new InvalidFrameException("Failed to decode argument list: " + e.toString());
        }
      }
    }

  }
}
