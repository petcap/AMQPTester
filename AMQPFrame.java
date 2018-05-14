/*
* This class represents an AMQP frame
*/

import java.util.*;

public class AMQPFrame {

  //Frame type enumeration
  public enum AMQPFrameType {
    METHOD((byte) 0x01),
    HEADER((byte) 0x02),
    BODY((byte) 0x03),
    HEARTHBEAT((byte) 0x04);

    private byte frameType;

    AMQPFrameType(byte frameType) {
      this.frameType = frameType;
    }

    public byte get() {
      return frameType;
    }
  }

  //Type of frame
  public AMQPFrameType amqpFrameType;

  //Frame channel
  public int channel;

  //Payload of the inner frame
  public ByteArrayBuffer payload;
  public AMQPInnerFrame innerFrame;

  //Constructor
  AMQPFrame(AMQPFrameType amqpFrameType, int channel, ByteArrayBuffer payload, AMQPInnerFrame innerFrame) {
    this.amqpFrameType = amqpFrameType;
    this.channel = channel;
    this.payload = payload;
    this.innerFrame = innerFrame;
  }

  //Build an AMQPFrame from a packet received on the wire
  //Expects one complete frame
  public static AMQPFrame build(ByteArrayBuffer frame) throws InvalidFrameException {
    //Frame type
    AMQPFrameType type = null;

    //Make sure we got at least the frame type + length
    if (frame.length() < 3) throw new InvalidFrameException("Frame length is too short: " + frame.length());
    //System.out.println("Outer frame: " + frame.toHexString());

    //Iterate over all possible frame types and see if the frame type is valid
    for(AMQPFrameType t : AMQPFrameType.values()) {
      if (t.get() == frame.getByte(0)) {
        System.out.println("Building frame object, method: " + t.name());
        type = t; //Correct type
        break;
      }
    }

    //Did we find a valid frame type?
    if (type == null) {
      throw new InvalidFrameException("Invalid frame type " + (int) frame.getByte(0));
    }

    //Get the channel number and payload length
    ByteArrayBuffer channel = frame.getByteArrayBuffer(1, 3);
    ByteArrayBuffer length = frame.getByteArrayBuffer(3, 7);

    //Debug frame length and payload length
    //System.out.println("Frame received on channel: " + channel.toLong() + "\n" + channel.toHexString());
    //System.out.println("Frame contains payload length: " + length.toLong() + "\n" + length.toHexString());

    //Get the inner frame payload and create a corresponding object from it
    ByteArrayBuffer framePayload = frame.getByteArrayBuffer(7, 7 + (int) length.toLong());
    AMQPInnerFrame innerFrame = AMQPInnerFrame.build(framePayload, type);

    //Check that the payload length is correct (or at least long enough)
    //8 = type(1) + channel(2) + size(4) + frame-end(1)
    if ((frame.length() - 8) < (int) length.toLong()) {
      throw new InvalidFrameException("Packet too short");
    }

    //Make sure the last byte is 0xCE
    if (frame.getByte((int) length.toLong() + 7) != (byte) 0xce) {
      throw new InvalidFrameException("Invalid frame-end: Not 0xCE");
    }

    //Create and return a new Frame object
    return new AMQPFrame(
    type,
    (int) channel.toLong(),
    framePayload,
    innerFrame
    );
  }
}
