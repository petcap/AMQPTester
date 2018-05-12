/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPMethodFrame extends AMQPInnerFrame {
  //Class and method represented by this frame
  public ByteArrayBuffer amqpClass;
  public ByteArrayBuffer amqpMethod;

  //HashMap of all arguments
  //HashMap<String, SomeShit> hashMap = new HashMap();

  //Constructor
  AMQPMethodFrame(ByteArrayBuffer amqpClass, ByteArrayBuffer amqpMethod, ByteArrayBuffer argList) throws InvalidFrameException {
    //Copy the argument list buffer in case we wish to change data in it
    argList = argList.copy();

    //Assign given class and method
    this.amqpClass = amqpClass;
    this.amqpMethod = amqpMethod;

    System.out.println("Creating new AMQPMethodFrame, class: " + amqpClass.toLong() + ", method: " + amqpMethod.toLong());
    System.out.println(argList.toHexString());

    //Depending on which class and method, read different values:

    //Class: Connection
    if (amqpClass.toLong() == 10) {
      //Method: Start-OK
      if (amqpMethod.toLong() == 11) {
        //TODO: Create abstract data structure which can hold nested AMQP tables etc
        //Iterate over shit and add to structure until done, then return
        //Make sure to check that all required fields are present
      }
    }

  }
}
