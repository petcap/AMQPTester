//This tester performs no particular tests, but is rather used to make sure that
//the rest of the tester code works as intended

import java.util.*;

public class AMQPTesterSimple extends AMQPTester {


  //Constructor, we've just completed the handshake and the client now expects a
  //connection.start object
  AMQPTesterSimple() {
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
    start_arg.put(new AShortString("locales"), new ALongString("sv-SE"));

    AMQPMethodFrame start_method_frame = new AMQPMethodFrame(new AShortUInt(10), new AShortUInt(10), start_arg);

    AMQPFrame complete_frame = new AMQPFrame(AMQPFrame.AMQPFrameType.METHOD, new AShortUInt(0), start_method_frame);

    System.out.println("Sent CONNECTION.START");
  }

  void deliverFrame(AMQPFrame amqpFrame) {
    System.out.println("AMQPTesterSimple got a frame");
  }

  AMQPFrame getFrame() {
    System.out.println("AMQPTesterSimple delievered a frame");
    return null;
  }
};
