//This tester performs a handshake and includes various exotic data types
//in the server-properties field in order to see how the client handles them

import java.util.*;

public class AMQPTesterDataTypes extends AMQPTester {

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
  AMQPTesterDataTypes(AMQPConnection amqpConnection) {

    //Store reference to the AMQPConnection we are working with
    this.amqpConnection = amqpConnection;

    //Arguments in Connection.Start
    LinkedHashMap<AShortString, AMQPNativeType> start_arg = new LinkedHashMap<AShortString, AMQPNativeType>();

    //Properties of server-properties
    //FIXME: Include more headers?
    LinkedHashMap<AShortString, AMQPNativeType> server_props = new LinkedHashMap<AShortString, AMQPNativeType>();
    server_props.put(new AShortString("copyright"), new ALongString("Data type testing"));

    //Add various data types here
    AFieldTable fieldTable = new AFieldTable();

    //Build a field-array to encode inside the field array
    AFieldArray fieldArray = new AFieldArray();
    //fieldArray.append(new ABoolean(false));
    //fieldArray.append(new ALongUInt(123));
    //fieldArray.append(new AShortUInt(321));
    //fieldArray.append(new AShortString("Array SS"));
    //fieldArray.append(new ALongString("Array LS"));

    //Encode nested field table with many levels

    for(int i=0; i!=50; ++i) {
      //Create a new field-table
      AFieldTable tmp = new AFieldTable();

      //Add our current table into it
      tmp.append(new AShortString("nested-ft-"+i), fieldTable);

      //Update the pointer
      fieldTable = tmp;
    }

    //Encode data into the field-table
    fieldTable.append(new AShortString("inner-FA"), fieldArray);
    //fieldTable.append(new AShortString("inner-LS"), new ALongString("Field LS"));
    //fieldTable.append(new AShortString("inner-SS"), new AShortString("Field SS"));
    //fieldTable.append(new AShortString("inner-BOOL"), new ABoolean(false));
    //fieldTable.append(new AShortString("inner-LONG-UINT"), new ALongUInt(123));
    //fieldTable.append(new AShortString("inner-SHORT-UINT"), new AShortUInt(123));

    //PoC - Encode a rogue short string with arbitrary data:
    //RabbitMQ uses the encoding tag for short strings (lower case s) for 16 bits
    //signed integers. This can (maybe?) be exploited to allow an attacker with
    //access to any field/array encoded data set to inject arbitrary protocol data
    //try {
    //  fieldTable.append(new AShortString("arbitrary"), new AShortString(ByteArrayBuffer.build(
    //    new ByteArrayBuffer(new byte[]{22, (byte) 0xFF}), //Short string length & first char, or 16 bit integer in RabbitMQ

    //    //RabbitMQ compatible clients will have decoded the above two bytes as a
    //    //signed 16 bit int, we are now free to use the rest of the string to
    //    //inject data as we wish.
    //    new ByteArrayBuffer(new byte[]{7}), //Injected key length
    //    new ByteArrayBuffer("int-str"), //Injected key
    //    new ByteArrayBuffer("S"), //Injected data type, S = long string
    //    new ByteArrayBuffer(new byte[]{0x00, 0x00, 0x00, 0x08}), //Long string length
    //    new ByteArrayBuffer("overflow") //Injected string contents
    //  )));
    //} catch (InvalidTypeException e) {
    //  System.err.println("Could not encode arbitrary short string: " + e.toString());
    //}

    //Encode the field table (and all of the above) into the handshake
    server_props.put(new AShortString("inner-FT"), fieldTable);

    //Specially encoded UTF-8 testing bytes
    //This forms one character under UTF8, but uses 2 octets over the wire
    ByteArrayBuffer utf8 = new ByteArrayBuffer(new byte[]{
      0x00, 0x00, 0x00, 0x02, //Long string length = 2
      (byte) 0b11011001, //Octet 1 (UTF-8 multibyte char, the two MSBs indicate two bytes)
      (byte) 0b10111111 //Octet 2 (Char continued)
    });

    try {
      //Add specially encoded UTF-8 string
      //server_props.put(new AShortString("utf8-test"), new ALongString(utf8));
      //System.out.println("Sending special UTF-8 chars");
    } catch(Exception e) {
      System.err.println("UTF8 data encoding failed: " + e.toString());
      System.exit(1);
    }

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
          arguments.put(new AShortString("channel-max"), new AShortUInt(100)); //No specific channel limit
          arguments.put(new AShortString("frame-max"), new ALongUInt(1024)); //TODO: Write test case for this; clients tends to accept this value but does not honor it later on
          arguments.put(new AShortString("heartbeat"), new AShortUInt(0));

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
        System.out.println("AMQPTesterDataTypes: Received bad frame during initialization");
      }
    }
  }

  //Periodical update
  public void periodical() {}

  //Currently triggered upon modifying the incoming frame queue
  //May be periodically triggered in the future
  public void updateState() {
    //Are we initializing? This is handeled separately to make the code more clean
    if (state == State.INITIALIZING) {
      updateInitializing();
      return;
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
      //System.out.println("AMQPTesterDataTypes sent a frame");
      return queue_outgoing.pop();
    }

    return null;
  }
};
