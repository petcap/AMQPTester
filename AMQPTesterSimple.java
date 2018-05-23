//This tester performs no particular tests, but is rather used to make sure that
//the rest of the tester code works as intended

import java.util.*;

public class AMQPTesterSimple extends AMQPTester {

  //Queue of outgoing frames
  LinkedList<AMQPFrame> queue_outgoing = new LinkedList<AMQPFrame>();

  //Constructor, we've just completed the handshake and the client now expects a
  //connection.start object
  AMQPTesterSimple() {
    System.out.println("AMQPTesterSimple initialized");

    //Arguments in Connection.Start
    LinkedHashMap<AShortString, AMQPNativeType> start_arg = new LinkedHashMap<AShortString, AMQPNativeType>();

    //Properties of server-properties
    //FIXME: Include more headers?
    LinkedHashMap<AShortString, AMQPNativeType> server_props = new LinkedHashMap<AShortString, AMQPNativeType>();
    server_props.put(new AShortString("copyright"), new ALongString("Hello World Inc."));

    //Add the expected data to the Connection.Start arglist
    start_arg.put(new AShortString("version-major"), new AOctet(0x00));
    start_arg.put(new AShortString("version-minor"), new AOctet(0x09));
    start_arg.put(new AShortString("server-properties"), new AFieldTable(server_props));
    start_arg.put(new AShortString("mechanisms"), new ALongString("Helloooo"));
    start_arg.put(new AShortString("locales"), new ALongString("en-US"));

    //Build the inner frame
    AMQPMethodFrame method_frame = new AMQPMethodFrame(new AShortUInt(10), new AShortUInt(10), start_arg);

    //Build the complete frame
    AMQPFrame complete_frame = new AMQPFrame(AMQPFrame.AMQPFrameType.METHOD, new AShortUInt(0), method_frame);

    //Queue the frame up to be sent to the client
    queue_outgoing.add(complete_frame);
  }

  //Called when a frame has been received and decoded over the wire
  public void deliverFrame(AMQPFrame amqpFrame) {
    System.out.println("AMQPTesterSimple got a frame: " + amqpFrame.amqpFrameType.name());
  }

  //Get a frame from the internal queue
  //Returns null if no frames are available
  public AMQPFrame getFrame() {
    if (queue_outgoing.size() != 0) {
      System.out.println("AMQPTesterSimple sent a frame");
      return queue_outgoing.pop();
    }

    return null;
  }
};
