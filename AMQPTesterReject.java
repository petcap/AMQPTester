//This is a specific tester which checks if an AMQP library handles rejected
//messages. Please only send message with the mandatory flag set to this tester

import java.util.*;

public class AMQPTesterReject extends AMQPTester {

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

  //Exchange and routing keys as requested by the client
  public AShortString exchange;
  public AShortString routingKey;

  //Received header frame, we need to store this since we are returning it later
  //when basic.return is sent back to the client
  public AMQPFrame receivedHeaderFrame;

  //Constructor, we've just completed the handshake and the client now expects a
  //connection.start object
  AMQPTesterReject(AMQPConnection amqpConnection) {

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
        arguments.put(new AShortString("channel-max"), new AShortUInt(1));
        arguments.put(new AShortString("frame-max"), new ALongUInt(1024*1024));
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

    //Pop a frame from the queue
    AMQPFrame frame = queue_incoming.pop();

    //Did we receive a Method frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.METHOD) {

      //Get the inner frame that contains all important frame data
      AMQPMethodFrame inner = (AMQPMethodFrame) frame.innerFrame;
      System.out.println("Frame received (full size: " + frame.size() + "): " + inner.toString());

      //Basic.publish
      if (inner.amqpClass.toInt() == 60 && inner.amqpMethod.toInt() == 40) {
        //Print warning if mandatory flag is not set (it should be in this test)
        if ( ((AOctet) inner.getArg("mandatory")).toInt() != 1) {
          System.out.println(" *** This test demands the mandatory flag set, make sure the client sets it and restart the test.");
        }

        //Store the exchange and routing key for later use in basic.return
        this.exchange = (AShortString) inner.getArg("exchange-name");
        this.routingKey = (AShortString) inner.getArg("routing-key");
      }

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
        //Build the channel.open-ok frame
        //Arguments: class, method, args (arg in this case is reserved)
        AMQPFrame outgoing = AMQPMethodFrame.build(20, 11, new ALongUInt(0));

        //Reply on same channel as we got the message on
        outgoing.channel = frame.channel;

        //Queue frame to be sent
        queue_outgoing.add(outgoing);

        //Debugging
        System.out.println("Sending Channel.Open-OK (for channel " + frame.channel + ")");
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
      }

      //Queue.declare
      if (inner.amqpClass.toInt() == 50 && inner.amqpMethod.toInt() == 10) {
        //List of arguments to be returned
        LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();

        //Queue name received from the client
        AShortString queue_name = (AShortString) inner.getArg("queue-name");

        //According to page 6 in the XML-derived specification, the queue-name
        //may be empty. In that case, the server should assign the last queue name
        //declared on the active vhost & channel automatically.
        if (queue_name.toString().equals("")) {

          //Generate a queue named based on the channel it was received on
          queue_name = new AShortString("amq.autoName_" + frame.channel.toString());

          //Print info about the queue name
          System.out.println("*** No queue name specified, returning: " + queue_name.toString());
        }

        //Add arguments
        arguments.put(new AShortString("queue"), queue_name);
        arguments.put(new AShortString("message-count"), new ALongUInt(0));
        arguments.put(new AShortString("consumer-count"), new ALongUInt(1));

        //Build frame and set same channel
        AMQPFrame outgoing = AMQPMethodFrame.build(50, 11, arguments);
        outgoing.channel = frame.channel;

        //Send queue.declare-ok
        queue_outgoing.add(outgoing);

        System.out.println("Sending Queue.Declare-OK");
      }
    }

    //Did we receive a Header frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.HEADER) {
      System.out.println("Received header frame in TesterReject (saving this for basic.return)");
      System.out.println(frame.innerFrame.toString());

      //Store the header frame for future use when sending basic.return
      receivedHeaderFrame = frame;
    }

    //Did we receive a Body frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.BODY) {
      System.out.println("Received body frame (full size: " + frame.toWire().length() + ") in TesterSimple, data:");
      System.out.println(frame.innerFrame.toString());

      //Since the whole point of this tester is to reject messages, we simply
      //reply with Basic.Return once receiving a body frame
      //List of arguments to be returned
      LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();

      //At this point, we should have set the exchange name and routing key in the above
      //code, but let's check just in case
      if (exchange == null || routingKey == null) {
        System.out.println("Fatal error: Exchange and/or routing key not received, closing connection");
        amqpConnection.status = AMQPConnection.AMQPConnectionState.DISCONNECT;
        return;
      }

      //Add arguments
      arguments.put(new AShortString("reply-code"), new AShortUInt(541)); //541 = Internal server error
      arguments.put(new AShortString("reply-text"), new AShortString("Testing message reject"));
      arguments.put(new AShortString("exchange"), exchange); //Stored previously
      arguments.put(new AShortString("routing-key"), routingKey); //Stored previously

      //Build frame and set same channel (basic=60, return=50)
      AMQPFrame outgoing = AMQPMethodFrame.build(60, 50, arguments);
      outgoing.channel = frame.channel;

      //Send basic.return
      queue_outgoing.add(outgoing);

      //Return the header and body frames too
      queue_outgoing.add(receivedHeaderFrame);
      queue_outgoing.add(frame);

      System.out.println("Sending Basic.Return");
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
