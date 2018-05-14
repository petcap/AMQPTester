/*
* This class represents an AMQP inner frame
* Class is never instantiated, but extended by other frame classes representing
* method-, header-, body- and hearthbeat frames
*/

public class AMQPInnerFrame {

  AMQPInnerFrame() {}
  AMQPInnerFrame(ByteArrayBuffer byteArrayBuffer) {}

  //Build an AMQPInnerFrame from a packet received on the wire
  //Expects one complete frame
  public static AMQPInnerFrame build(ByteArrayBuffer byteArrayBuffer, AMQPFrame.AMQPFrameType frameType) throws InvalidFrameException {
    //Create a copy of the ByteArrayBuffer since we're going to modify it
    byteArrayBuffer = byteArrayBuffer.copy();

    //Do we want to create a Method frame?
    if (frameType == AMQPFrame.AMQPFrameType.METHOD) {
      System.out.println("AMQPInnerFrame building new Method frame from this buffer:");
      System.out.println(byteArrayBuffer.toHexString());

      //Read class and method
      ByteArrayBuffer amqpClass = byteArrayBuffer.pop(2); //Pop 2 bytes
      //System.out.println("Assigning class : " + amqpClass.toLong());
      ByteArrayBuffer amqpMethod = byteArrayBuffer.pop(2); //Pop 2 bytes
      //System.out.println("Assigning method: " + amqpMethod.toLong());

      //Read first 4 octets from the arguments list
      //Specified as an unsigned long, but the specification never mentions
      //what this value should contain. RabbitMQ populates it with the argument
      //length, but it could be pretty much anything
      //The PHP library seems to be using some other interpretation of it, so
      //we'll simply ignore it here because we already know the length anyway
      ByteArrayBuffer uselessLong = byteArrayBuffer.pop(4);

      System.out.println("Useless array long value:");
      System.out.println(uselessLong.toHexString());

      //From now on, we need to start reading the method arguments
      //The structure of each argument list differs depending on which class
      //and method we're dealing with
      AMQPMethodFrame amqpMethodFrame = new AMQPMethodFrame(
        amqpClass,
        amqpMethod,
        byteArrayBuffer
      );

      return amqpMethodFrame;
    }

    throw new InvalidFrameException("Unknown frame type, this should never happen");
  }
}
