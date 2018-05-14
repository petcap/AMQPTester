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
    //Copy the argument list buffer in case we wish to change data in it
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
        //arguments.put("");
      }
    }

  }
}
