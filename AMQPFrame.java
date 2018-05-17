/*
* This class represents an AMQP frame
*/

import java.util.*;

public class AMQPFrame {

  //Frame type enumeration including the frame level octet
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

    public ByteArrayBuffer toWire() {
      return new ByteArrayBuffer(frameType);
    }
  }

  //Type of frame
  public AMQPFrameType amqpFrameType;

  //Frame channel
  public AShortUInt channel;

  //The inner frame
  public AMQPInnerFrame innerFrame;

  //Constructor
  AMQPFrame(AMQPFrameType amqpFrameType, AShortUInt channel, AMQPInnerFrame innerFrame) {
    this.amqpFrameType = amqpFrameType;
    this.channel = channel;
    this.innerFrame = innerFrame;
  }

  public ByteArrayBuffer toWire() {
    //Frame format:
    //Type(1 octet) + Channel(2 octets) + Payload length(4 octets) + Actual payload + 0xCE
    ByteArrayBuffer ret = new ByteArrayBuffer();

    //Add type to frame
    ret.put(amqpFrameType.toWire());

    //Add channel
    ret.put(channel.toWire());

    //Inner frame in wire format
    ByteArrayBuffer inner = innerFrame.toWire();

    //Add payload length
    ret.put(new ALongUInt(inner.length()).toWire());

    //Add actual payload
    ret.put(inner);

    //Add EOP (End of Packet)
    ret.put(new byte[] {(byte) 0xce });

    return ret;
  }

  //Build an AMQPFrame from a packet received on the wire
  //Expects one complete frame
  public static AMQPFrame build(ByteArrayBuffer frame) throws InvalidFrameException {
    //Frame type
    AMQPFrameType type = null;

    //Make sure we got at least the frame type + length
    if (frame.length() < 3) throw new InvalidFrameException("Frame length is too short: " + frame.length());
    //System.out.println("Outer frame: " + frame.toHexString());

    //Iterate over all possible frame types and see what type of frame we got
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

    //We have found the type, remove the corresponding byte from the buffer
    frame.deleteFront(1);

    AShortUInt channel;
    ALongUInt length;

    //Get the channel number and payload lenth
    try {
      channel = new AShortUInt(frame);
      length = new ALongUInt(frame);
    } catch (InvalidTypeException e) {
      throw new InvalidFrameException("AMQPFrame: Failed to decode channel or payload length: " + e.toString());
    }

    //Debug frame length and payload length
    System.out.println("Frame received on channel: " + channel.toString());
    System.out.println("Frame contains payload length: " + length.toString());

    //Check that the buffered payload length is long enough
    if (frame.length() < 1 + length.toInt()) { //Add 1 because of ending trailer byte 0xCE
      throw new InvalidFrameException("Packet too short");
    }

    //Pop the frame contents
    ByteArrayBuffer framePayload = frame.pop(length.toInt());

    //Check that we have an EOP (End of Packet) bytes
    if (!frame.pop(1).equals(new ByteArrayBuffer(new byte[]{(byte) 0xCE}))) {
      throw new InvalidFrameException("Frame does not end with 0xCE");
    }

    //All checks on the frame OK, build the inner frame
    AMQPInnerFrame innerFrame = AMQPInnerFrame.build(framePayload, type);

    //Create and return a new Frame object
    return new AMQPFrame(
      type,
      channel,
      //framePayload, //We don't care about the original payload since it is redundant
      innerFrame
    );
  }
}
