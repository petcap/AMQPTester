//This tester performs no particular tests, but is rather used to make sure that
//the rest of the tester code works as intended

import java.util.*;

public class AMQPTesterSimple extends AMQPTester {

  //Queue of incoming frames
  LinkedList<AMQPFrame> queue_incoming = new LinkedList<AMQPFrame>();

  //Queue of outgoing frames
  LinkedList<AMQPFrame> queue_outgoing = new LinkedList<AMQPFrame>();

  //Tester state enumeration
  public enum State {
    INITIALIZING, //Connection.Start, Connection.Tune
    HANDSHAKE_COMPLETE, //Connection.Start and Tune complete
  }

  //Associated AMQPConnection
  AMQPConnection amqpConnection;

  //Current state this tester is in
  public State state = State.INITIALIZING;

  //Constructor, we've just completed the handshake and the client now expects a
  //connection.start object
  AMQPTesterSimple(AMQPConnection amqpConnection) {

    //Store reference to the AMQPConnection we are working with
    this.amqpConnection = amqpConnection;

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

    //Build the complete frame
    AMQPFrame start_frame = AMQPMethodFrame.build(10, 10, start_arg);

    //Queue the frame up to be sent to the client
    queue_outgoing.add(start_frame);
    System.out.println("Sending Connection.Start");
  }

  //Called when a frame is received and we are still initalizing
  public void updateInitializing() {
    if (queue_incoming.size() != 0) {
      //Get the received frame
      AMQPFrame frame = queue_incoming.pop();

      //Did we receive a method frame?
      if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.METHOD) {
        //Get the inner frame, which is an AMQPMethodFrame in this case
        AMQPMethodFrame inner = (AMQPMethodFrame) frame.innerFrame;
        System.out.println("AMQPTesterSimple received: " + inner.toString());

        //Start-OK
        if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 11) {
          //Send Connection.Tune

          //Arguments to include in the method call
          LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();
          arguments.put(new AShortString("channel-max"), new AShortUInt(1));
          arguments.put(new AShortString("frame-max"), new ALongUInt(1000));
          arguments.put(new AShortString("heartbeat"), new AShortUInt(10));

          //Send connection.tune
          queue_outgoing.add(AMQPMethodFrame.build(10, 30, arguments));
          System.out.println("Sending Connection.Tune");
        }

        //Connection.Tune-ok
        if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 31) {
          state = State.HANDSHAKE_COMPLETE;
          System.out.println("Handshake phase complete");
        }

      } else { //We are not expecting any other frame types...
        //Invalid frame, disconnect the client
        amqpConnection.status = AMQPConnection.AMQPConnectionState.DISCONNECT;
        System.out.println("AMQPTesterSimple: Received bad frame during initialization");
      }
    }
  }

  //Currently triggered upon modifying the incoming frame queue
  //May be periodically triggered in the future
  public void updateState() {
    //Are we initializing? This is handeled separately to make the code more clean
    if (state == State.INITIALIZING) {
      updateInitializing();
      return;
    }

    //Make sure that we have incoming data
    if (queue_incoming.size() == 0) return;

    AMQPFrame frame = queue_incoming.pop();

    //Did we receive a Method frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.METHOD) {

      //Get the inner frame that contains all important frame data
      AMQPMethodFrame inner = (AMQPMethodFrame) frame.innerFrame;
      System.out.println("AMQPTesterSimple(inited) received: " + inner.toString());

      //Connection.open
      if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 40) {
        //Maybe check the path in the future if needed?
        //Send connection.open-ok
        //The supplied octet is the reserved field
        queue_outgoing.add(AMQPMethodFrame.build(10, 41, new AOctet(0x00)));
        System.out.println("Sending Connection.Open-OK");
      }

      //Channel.open
      if (inner.amqpClass.toInt() == 20 && inner.amqpMethod.toInt() == 10) {
        //Build the channel.open-ok frame
        //Arguments: class, method, args (arg in this case is reserved)
        AMQPFrame outgoing = AMQPMethodFrame.build(20, 11, new ALongUInt(0));

        //Reply on same channel as we got the message on
        outgoing.channel = frame.channel;

        //Queue frame to be sent
        queue_outgoing.add(outgoing);

        //Debugging
        System.out.println("Sending Channel.Open-OK");
      }

      //Queue.declare
      if (inner.amqpClass.toInt() == 50 && inner.amqpMethod.toInt() == 10) {
        //List of arguments to be returned
        LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();

        //Queue name received from the client
        AShortString queue_name = (AShortString) inner.getArg("queue-name");

        //Add arguments
        arguments.put(new AShortString("queue"), queue_name);
        arguments.put(new AShortString("message-count"), new ALongUInt(0));
        arguments.put(new AShortString("consumer-count"), new ALongUInt(0));

        //Build frame and set same channel
        AMQPFrame outgoing = AMQPMethodFrame.build(50, 11, arguments);
        outgoing.channel = frame.channel;

        //Send queue.declare-ok
        queue_outgoing.add(outgoing);

        System.out.println("Sending Queue.Declare-OK");
      }

      //Basic.consume
      if (inner.amqpClass.toInt() == 60 && inner.amqpMethod.toInt() == 20) {
        //Build frame
        //Short string = consumer ID
        AMQPFrame outgoing = AMQPMethodFrame.build(60, 21, new AShortString("HelloWorld"));

        //Set same channel
        outgoing.channel = frame.channel;

        //Send
        System.out.println("Sending Basic.Consume-OK");
        System.out.println(outgoing.toWire().toHexString());
      }
    }
  }

  //Called when a frame has been received and decoded over the wire
  public void deliverFrame(AMQPFrame amqpFrame) {

    //Add frame to queue
    queue_incoming.add(amqpFrame);

    //Trigger state update
    updateState();
  }

  //Get a frame from the internal queue
  //Returns null if no frames are available
  public AMQPFrame getFrame() {
    if (queue_outgoing.size() != 0) {
      //System.out.println("AMQPTesterSimple sent a frame");
      return queue_outgoing.pop();
    }

    return null;
  }
};
