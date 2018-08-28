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
    HEARTBEAT((byte) 0x08); //This is ambigiuos; should be 4 according to spec but everyone uses 8

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

  //Get frame size on wire (full frame, incl. all header values
  public int size() {
    return this.toWire().size();
  }

  //Create a ByteArrayBuffer containing the frame
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

    //Add EOP (End of Packet) byte
    ret.put(new byte[] { (byte) 0xce });

    return ret;
  }

  //Checks if the ByteArrayBuffer contains one (or more) frame ready to decode
  //using the build() method. This method only validates that the buffer contains
  //the complete frame as specified by the frame header length, it does not
  //validate that the frame is correctly encoded
  public static boolean hasFullFrame(ByteArrayBuffer byteArrayBuffer) {
    //Minimal possible length of a frame
    if (byteArrayBuffer.length() < 8) {
      return false;
    }

    //Make a copy since we do not want to modify the original buffer
    ByteArrayBuffer frame = byteArrayBuffer.copy();

    //We do not care about the frame type or channel, delete first 3 octets
    frame.deleteFront(3);

    //Pop the length of the frame
    ALongUInt length;
    try {
      length = new ALongUInt(frame);
    } catch(InvalidTypeException e) {
      System.out.println("hasFullName(): Type Exception, should not happen");
      return false; //Should never happen since we check the length
    }

    //Calculate the expected length of the buffer
    //Frame content length + Frame type + Channel + Frame length + Trailing 0xCE
    long expectedLength = length.toLong() + (long) 1 + (long) 2 + (long) 4 + (long) 1;

    //System.out.println("hasFullFrame(): Expected length: " + expectedLength + ", actual: " + byteArrayBuffer.length());

    //Check if we have received the complete frame
    return (expectedLength <= byteArrayBuffer.length());
  }

  //Build an AMQPFrame from a frame received on the wire
  //Expects (at least) one complete frame
  //This method processes any of the four frame types
  //The ByteArrayBuffer will have the frame popped
  //Throws InvalidFrameException if there exist no valid frame in the buffer
  public static AMQPFrame build(ByteArrayBuffer frame) throws InvalidFrameException {
    //Frame type
    AMQPFrameType type = null;

    //A frame is at least 8 octets long:
    //1 octet frame type
    //2 octet Channel
    //4 octet frame length
    //1 octet frame ending 0xCE
    if (frame.length() < 8) throw new InvalidFrameException("Frame length is too short: " + frame.length());
    //System.out.println("Outer frame: " + frame.toHexString());

    //Iterate over all possible frame types and see what type of frame we got
    for(AMQPFrameType t : AMQPFrameType.values()) {
      if (t.get() == frame.getByte(0)) {
        //System.out.println("Building frame object, method: " + t.name());
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
    //System.out.println("Frame received on channel: " + channel.toString());
    //System.out.println("Frame contains payload length: " + length.toString());

    //Check that the buffered payload length is long enough
    if (frame.length() < 1 + length.toInt()) { //Add 1 because of ending trailer byte 0xCE
      throw new InvalidFrameException("Frame too short");
    }

    //Pop the frame contents
    ByteArrayBuffer framePayload = frame.pop(length.toInt());

    //Check that we have an EOP (End of Packet) byte
    if (!frame.pop(1).equals(new ByteArrayBuffer(new byte[]{(byte) 0xCE}))) {
      throw new InvalidFrameException("Frame does not end with 0xCE");
    }

    //All checks on the frame OK, build the inner frame
    AMQPInnerFrame innerFrame = AMQPInnerFrame.build(framePayload, type, length, channel);

    //Create and return a new Frame object
    return new AMQPFrame(
      type,
      channel,
      innerFrame
    );
  }
}
