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
      //This method consumes data from the ByteArrayBuffer, make sure to copy data
      //if you need to keep it for other purposes
      public static AMQPInnerFrame build(ByteArrayBuffer byteArrayBuffer, AMQPFrame.AMQPFrameType frameType) throws InvalidFrameException {
        //Do we want to create a Method frame?
        if (frameType == AMQPFrame.AMQPFrameType.METHOD) {
          //System.out.println("AMQPInnerFrame building new Method frame from this buffer:");
          //System.out.println(byteArrayBuffer.toHexString());

          //Declare and set to null so compiler wont complain
          AShortUInt amqpClass = null;
          AShortUInt amqpMethod = null;

          //Read class and method
          try {
            amqpClass = new AShortUInt(byteArrayBuffer); //Pop 2 bytes
            amqpMethod = new AShortUInt(byteArrayBuffer); //Pop 2 bytes
          } catch (InvalidTypeException e) {
            throw new InvalidFrameException("Failed to read method frame class/method: " + e.toString());
          }

          AMQPMethodFrame amqpMethodFrame = null;

          //From now on, we need to start reading the method arguments
          //The structure of each argument list differs depending on which class
          //and method we're dealing with
          try {
            amqpMethodFrame = new AMQPMethodFrame(
            amqpClass,
            amqpMethod,
            byteArrayBuffer
            );
          } catch (InvalidTypeException e) {
            throw new InvalidFrameException("Failed to build method frame, invalid encoding: " + e.toString());
          }

          return amqpMethodFrame;
        }

        //Should never be reached
        throw new InvalidFrameException("Unknown frame type received (probably a bug in the tester code)");
      }

      //Output data to wire
      public ByteArrayBuffer toWire() {
        System.err.println("Override me");
        System.exit(1);
        return null;
      }
    };
