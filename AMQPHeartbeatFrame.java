/*
* This class represents an AMQP Heartbeat frame
* A complete outer + inner frame of a heartbeat looks SHOULD like this:
* 0x04         type, 4=Heartbeat
* 0x0000       Channel, always zero
* 0x00000003   Length, always 3 (because inner frame is always 3)
* 0x080000     Inner frame
* 0xCE         End-of-frame
*
* However, it seems most implementations (RabbitMQ, Qpid, etc) only sends an
* empty outer frame with type=8, i.e.:
* 0x08         type, 8=Heartbeat
* 0x0000       Channel, always zero
* 0x00000000   Length, always 0 (because inner frame is always 0)
* -            Inner frame (empty)
* 0xCE         End-of-frame
*/

import java.util.*;

public class AMQPHeartbeatFrame extends AMQPInnerFrame {

  //Constructor for creating frame from wire
  //This constructor is called from AMQPInnerFrame.build()
  AMQPHeartbeatFrame(ALongUInt length, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {
    //A Heartbeat inner frame always has the value 0x080000 according to the speification
    if (length.toInt() != 0) {
      throw new InvalidFrameException("Invalid Heartbeat length, expecting 3, got: " + length.toInt());
    }
  }

  //Constructor to create object programmatically
  AMQPHeartbeatFrame() {}

  //Build a new Heartbeat frame
  //Channel has to be zero, but for fuzzing reasons it can still be set to something else
  public static AMQPFrame build(AShortUInt channel) {
    //Create the new frame
    AMQPFrame frame = new AMQPFrame(AMQPFrame.AMQPFrameType.HEARTBEAT, channel, new AMQPHeartbeatFrame());

    //Set the channel
    frame.channel = channel;

    //Build the complete frame
    return frame;
  }

  //Build a Heartbeat frame on standard channel
  public static AMQPFrame build() {
    return build(new AShortUInt(0));
  }

  //For debugging
  public String toString() {
    return "Heartbeat frame";
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    return new ByteArrayBuffer(new byte[]{});
  }
};
