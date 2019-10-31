//This is a specific tester which checks if an AMQP library honors the channel limits
//as specificed in the initial handshake

import java.util.*;

/*
 * This test checks that a client does not open more than no_channels channels to the broker
 * The client under test is sent a maximum channel limit of no_channels during connection and
 * the client under test should attempt to open as many channels as possible in order to see
 * if the limit is honored
 */

public class AMQPTesterChannels extends AMQPTester {

  //The server will send this channel limit to the client
  public static final int no_channels = 1000;

  //The current count of open channels
  public int open_channels = 0;

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
  AMQPTesterChannels(AMQPConnection amqpConnection) {

    //Store reference to the AMQPConnection we are working with
    this.amqpConnection = amqpConnection;

    //Arguments in Connection.Start
    LinkedHashMap<AShortString, AMQPNativeType> start_arg = new LinkedHashMap<AShortString, AMQPNativeType>();

    //Properties of server-properties
    LinkedHashMap<AShortString, AMQPNativeType> server_props = new LinkedHashMap<AShortString, AMQPNativeType>();
    server_props.put(new AShortString("copyright"), new ALongString("Hello World Inc."));

    //Add the expected data to the Connection.Start arglist
    start_arg.put(new AShortString("version-major"), new AOctet(0x00));
    start_arg.put(new AShortString("version-minor"), new AOctet(0x09));
    start_arg.put(new AShortString("server-properties"), new AFieldTable(server_props));
    start_arg.put(new AShortString("mechanisms"), new ALongString("PLAIN AMQPPLAIN"));
    start_arg.put(new AShortString("locales"), new ALongString("en-US"));

    //Build the complete frame
    AMQPFrame start_frame = AMQPMethodFrame.build(10, 10, start_arg);

    //Queue the frame up to be sent to the client
    queue_outgoing.add(start_frame);
    System.out.println("Sending Connection.Start");
  }

  //Called when a frame is received and we are still initalizing
  public void updateInitializing() {
    //Make sure we have at least one frame
    if (queue_incoming.size() == 0) return;
    //Get the received frame
    AMQPFrame frame = queue_incoming.pop();

    //Did we receive a method frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.METHOD) {
      //Get the inner frame, which is an AMQPMethodFrame in this case
      AMQPMethodFrame inner = (AMQPMethodFrame) frame.innerFrame;
      System.out.println("Received: " + inner.toString());

      //Start-OK
      if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 11) {
        //Send Connection.Tune

        //Arguments to include in the method call
        LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();
        arguments.put(new AShortString("channel-max"), new AShortUInt(no_channels));
        arguments.put(new AShortString("frame-max"), new ALongUInt(4096));
        arguments.put(new AShortString("heartbeat"), new AShortUInt(0)); //Ignore heartbeats for now

        //Send connection.tune
        queue_outgoing.add(AMQPMethodFrame.build(10, 30, arguments));
        System.out.println("Sending Connection.Tune");
      }

      //Connection.Tune-ok
      if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 31) {
        state = State.HANDSHAKE_COMPLETE;
        System.out.println("Handshake phase complete");
      }

    } else { //We are not expecting any non-method frames here
      //Invalid frame, disconnect the client
      amqpConnection.status = AMQPConnection.AMQPConnectionState.DISCONNECT;
      System.out.println("AMQPTesterChannels: Received bad frame during initialization");
    }
  }

  //Periodical update
  public void periodical() {

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
      System.out.println("Frame received (full size: " + frame.size() + "): " + inner.toString());

      //Connection.open
      if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 40) {
        //Maybe check the path in the future if needed?
        //Send connection.open-ok
        //The supplied octet is the reserved field
        queue_outgoing.add(AMQPMethodFrame.build(10, 41, new AOctet(0x00)));
        System.out.println("Sending Connection.Open-OK");
      }

      //Connection.close
      if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 50) {
        //Maybe check the path in the future if needed?
        //Send connection.open-ok
        //The supplied octet is the reserved field
        queue_outgoing.add(AMQPMethodFrame.build(10, 51));
        System.out.println("Sending Connection.Close-OK");
      }

      //Channel.open
      if (inner.amqpClass.toInt() == 20 && inner.amqpMethod.toInt() == 10) {
        //Check that we are under the channel limit
        if (open_channels >= no_channels) {
          System.out.println("*** WARNING: Over channel limit, but still requesting more channels");

          //Drop the client
          amqpConnection.status = AMQPConnection.AMQPConnectionState.DISCONNECT;
          return;
        }

        //Build the channel.open-ok frame
        //Arguments: class, method, args (arg in this case is reserved)
        AMQPFrame outgoing = AMQPMethodFrame.build(20, 11, new ALongUInt(0));

        //Reply on same channel as we got the message on
        outgoing.channel = frame.channel;

        //Queue frame to be sent
        queue_outgoing.add(outgoing);

        //Increase channel number
        open_channels += 1;

        //Debugging
        System.out.println("Sending Channel.Open-OK (Currently open: " + open_channels + ", last opened: " + frame.channel.toString() + ")");
      }

      //Channel.close
      if (inner.amqpClass.toInt() == 20 && inner.amqpMethod.toInt() == 40) {
        //Prepare channel.close-ok
        AMQPFrame outgoing = AMQPMethodFrame.build(20, 41, new ALongUInt(0));

        //Reply on same channel as we got the message on
        outgoing.channel = frame.channel;

        //Queue frame to be sent
        queue_outgoing.add(outgoing);

        //Debugging
        System.out.println("Sending Channel.Close-OK");

        //Decrease channel number
        open_channels -= 1;
      }
    }

    //Did we receive a Header frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.HEADER) {
      System.out.println("Received header frame in TesterChannels, not interested...");
      System.out.println(frame.innerFrame.toString());
    }

    //Did we receive a Body frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.BODY) {
      System.out.println("Received body frame (full size: " + frame.toWire().length() + ") in TesterSimple, data:");
      System.out.println(frame.innerFrame.toString());
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
      //System.out.println("AMQPTesterChannels sent a frame");
      return queue_outgoing.pop();
    }

    return null;
  }
};
