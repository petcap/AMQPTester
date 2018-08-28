//This tester expects a client to subscribe to a queue after which it
//will send heartbeats (both valid ones, and then invalid ones on channels other than zero)

import java.util.*;

public class AMQPTesterHeartbeat extends AMQPTester {

  //Queue of incoming frames
  LinkedList<AMQPFrame> queue_incoming = new LinkedList<AMQPFrame>();

  //Queue of outgoing frames
  LinkedList<AMQPFrame> queue_outgoing = new LinkedList<AMQPFrame>();

  //Tester state enumeration
  public enum State {
    INITIALIZING, //Connection.Start, Connection.Tune
    HANDSHAKE_COMPLETE, //Connection.Start and Tune complete
    SUBSCRIBED, //Client subscribed for messages
  }

  //Temporary counter
  int temp_count = 0;

  //Associated AMQPConnection
  AMQPConnection amqpConnection;

  //Current state this tester is in
  public State state = State.INITIALIZING;

  //Constructor, we've just completed the handshake and the client now expects a
  //connection.start object
  AMQPTesterHeartbeat(AMQPConnection amqpConnection) {

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
    start_arg.put(new AShortString("mechanisms"), new ALongString("PLAIN AMQPPLAIN")); //Not checked anyway
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
        System.out.println("Received: " + inner.toString());

        //Start-OK
        if (inner.amqpClass.toInt() == 10 && inner.amqpMethod.toInt() == 11) {
          //Send Connection.Tune

          //Arguments to include in the method call
          LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();
          arguments.put(new AShortString("channel-max"), new AShortUInt(0)); //No specific channel limit
          arguments.put(new AShortString("frame-max"), new ALongUInt(1024)); //TODO: Write test case for this; clients tends to accept this value but does not honor it later on
          arguments.put(new AShortString("heartbeat"), new AShortUInt(1)); //Heartbeat every 1 sec

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
        System.out.println("AMQPTesterHeartbeat: Received bad frame during initialization");
      }
    }
  }

  //Periodical update
  public void periodical() {
    //Only send periodical messages if the client is subscribing
    if (state != State.SUBSCRIBED) return;

    //First attempt to send a regular heartbeat which is OK according to the standard
    if (temp_count == 0) {
      //Build a standard heartbeat frame
      AMQPFrame hb = AMQPHeartbeatFrame.build();

      //Queue it up to be sent to the client
      queue_outgoing.add(hb);

      System.out.println("Sent heartbeat frame on channel 0: " + hb.toWire().toHexString());
      temp_count++;
      return;
    }

    //Second attempt to send a heartbeat which is broken (i.e. sent on channel 2)
    //Channel 1 is being used for subscribing to the queue
    if (temp_count == 1) {
      //Build a standard heartbeat frame
      AMQPFrame hb = AMQPHeartbeatFrame.build(new AShortUInt(2));

      //Queue it up to be sent to the client
      queue_outgoing.add(hb);

      System.out.println("Sent BROKEN heartbeat frame on channel 0: " + hb.toWire().toHexString());
      temp_count++;
      return;
    }

    if (temp_count == 2) {
      LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();
      arguments.put(new AShortString("consumer-tag"), new AShortString("amq.HelloWorld"));
      arguments.put(new AShortString("delivery-tag"), new ALongLongUInt(1));
      arguments.put(new AShortString("redelivered"), new ABoolean(false));
      arguments.put(new AShortString("exchange"), new AShortString("Heeey"));
      arguments.put(new AShortString("routing-key"), new AShortString("hello"));
      AMQPFrame outgoing = AMQPMethodFrame.build(60, 60, arguments);

      //Set same channel
      outgoing.channel = new AShortUInt(1);

      queue_outgoing.add(outgoing);
      System.out.println("Sending Basic.Deliver");

      //Send header frame
      AMQPFrame header = AMQPHeaderFrame.build(
        new AShortUInt(1), //Same channel as received on
        new AShortUInt(60), //Class ID 60
        new ALongLongUInt(23) //Body length
      );

      queue_outgoing.add(header);
      System.out.println("Sent header");

      //Send body frame
      AMQPFrame body = AMQPBodyFrame.build(
        new AShortUInt(1), //Same channel as received on
        "Periodical message number " + temp_count
      );

      //Queue the body frame
      queue_outgoing.add(body);
      System.out.println("Sent message AFTER sending broken heartbeat");
      temp_count++;
      return;
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

      //Basic.consume
      if (inner.amqpClass.toInt() == 60 && inner.amqpMethod.toInt() == 20) {

        //Build frame
        //Short string = consumer ID, unique to channel
        AMQPFrame outgoing = AMQPMethodFrame.build(60, 21, new AShortString("amq.HelloWorld"));

        //Set same channel
        outgoing.channel = frame.channel;

        queue_outgoing.add(outgoing);

        System.out.println("Sending Basic.Consume-OK");

        //We are now ready to send messages to the client, let's attempt to send a
        //message and then close the connection
        //Add arguments to basic.deliver
        LinkedHashMap<AShortString, AMQPNativeType> arguments = new LinkedHashMap<AShortString, AMQPNativeType>();
        arguments.put(new AShortString("consumer-tag"), new AShortString("amq.HelloWorld"));
        arguments.put(new AShortString("delivery-tag"), new ALongLongUInt(1));
        arguments.put(new AShortString("redelivered"), new ABoolean(false));
        arguments.put(new AShortString("exchange"), new AShortString("hello"));
        arguments.put(new AShortString("routing-key"), new AShortString("hello"));
        outgoing = AMQPMethodFrame.build(60, 60, arguments);

        //Set same channel
        outgoing.channel = frame.channel;

        queue_outgoing.add(outgoing);
        System.out.println("Sending Basic.Deliver");

        //Send header frame
        AMQPFrame header = AMQPHeaderFrame.build(
          frame.channel, //Same channel as received on
          new AShortUInt(60), //Class ID 60
          new ALongLongUInt(5) //Body length
        );

        queue_outgoing.add(header);
        System.out.println("Sent header");

        //Send body frame
        AMQPFrame body = AMQPBodyFrame.build(
          frame.channel, //Same channel as received on
          "Hello from AMQPTesterHeartbeat.java"
        );

        //Queue the body frame
        queue_outgoing.add(body);
        System.out.println("Sent body");

        //Our client is now waiting for messages
        state = State.SUBSCRIBED;
      }
    }

    //Did we receive a Header frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.HEADER) {
      System.out.println("Received header frame in TesterSimple");
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
      //System.out.println("AMQPTesterHeartbeat sent a frame");
      return queue_outgoing.pop();
    }

    return null;
  }
};
