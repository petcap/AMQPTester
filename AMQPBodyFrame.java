/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPBodyFrame extends AMQPInnerFrame {

  //The actual payload of the body frame
  public ByteArrayBuffer payload;

  //Constructor for creating frame from wire
  //We need the length in order to read enough data
  AMQPBodyFrame(ALongUInt length, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {
    //Check length constrains
    if (buffer.length() < length.toInt()) {
      throw new InvalidFrameException("Body frame contains less data than specified in the packet");
    }

    //Read payload
    payload = buffer.pop(length.toInt());
  }

  //Constructor from string
  AMQPBodyFrame(String data) {
    this.payload = new ByteArrayBuffer(data);
  }

  //Constructor from ByteArrayBuffer
  AMQPBodyFrame(ByteArrayBuffer data) {
    this.payload = data.copy();
  }

  //Build a new body frame
  public static AMQPFrame build(AShortUInt channel, ByteArrayBuffer content) {

    //Build the inner frame
    AMQPBodyFrame body_frame = new AMQPBodyFrame(content);

    //Build the complete frame
    return new AMQPFrame(AMQPFrame.AMQPFrameType.BODY, channel, body_frame);
  }

  //Build a new body frame from String
  public static AMQPFrame build(AShortUInt channel, String content) {
    return build(channel, new ByteArrayBuffer(content));
  }

  //For debugging
  public String toString() {
    String ret = "Body frame:\n";
    ret += " * Payload: " + payload.toString() + "\n";
    return ret;
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    return payload.copy();
  }
};
