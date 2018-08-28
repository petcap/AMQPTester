/*
* This class represents an AMQP Heartbeat frame
*/

import java.util.*;

public class AMQPHeartbeatFrame extends AMQPInnerFrame {



  //Constructor for creating frame from wire
  //We need the length in order to read enough data
  //This constructor is called from AMQPInnerFrame.build()
  AMQPHeartbeatFrame(ALongUInt length, ByteArrayBuffer buffer) throws InvalidFrameException, InvalidTypeException {
    //A Heartbeat inner frame always has the value 0x080000 according to the speification
    if (length.toInt() != 3) {
      throw new InvalidFrameException("Invalid Heartbeat length, expecting 3, got: " + length.toInt());
    }

    //Pop 3 bytes from the buffer and make sure it contains a heartbeat
    if (!buffer.pop(3).equals(new byte[]{0x08, 0x00, 0x00})) {
      throw new InvalidFrameException("Invalid Heartbeat content, got: " + buffer.toHexString());
    }
  }

  //For debugging
  public String toString() {
    return "Heartbeat frame";
  }

  //Generate a ByteArrayBuffer with the contents to be sent over the TCP connection
  public ByteArrayBuffer toWire() {
    return new ByteArrayBuffer(new byte[]{0x08, 0x00, 0x00});
  }
};
