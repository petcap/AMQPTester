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

    //Pop a frame from the queue
    AMQPFrame frame = queue_incoming.pop();

    //Did we receive a Method frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.METHOD) {
      //Get the inner frame, which is an AMQPMethodFrame in this case
      AMQPMethodFrame inner = (AMQPMethodFrame) frame.innerFrame;
      System.out.println("Received method frame: " + inner.toString());
    }

    //Did we receive a Header frame?
    if (frame.amqpFrameType == AMQPFrame.AMQPFrameType.HEADER) {
      System.out.println("Received header frame in TesterReject");
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
