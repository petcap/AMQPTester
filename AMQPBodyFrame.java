/*
* This class represents an AMQP specific inner frame
*/

import java.util.*;

public class AMQPBodyFrame extends AMQPInnerFrame {

  //Store the payload internally as a Long String
  //This has the advantage of automatically adding the 4 length octets when encoding/decoding
  public ByteArrayBuffer payload;

  //Constructor for creating frame from wire
  AMQPBodyFrame(ALongUInt length, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {
    //Read payload
    payload = buffer.pop(length.toInt());
  }

  //For debugging
  public String toString() {
    String ret = "Body frame:\n";
    ret += " * Data: " + payload.toString() + "\n";
    return ret;
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    return payload.copy();
  }
};
