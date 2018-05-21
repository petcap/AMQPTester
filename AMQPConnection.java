/*
* This class represents a connected AMQP client.
*/

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class AMQPConnection {

  public static enum AMQPConnectionState {
    UNINITIALIZED,        //Default state
    HANDSHAKE_COMPLETE,   //After handshake is complete
    START_SENT,           //After handshake is complete
    DISCONNECT            //When the server code should write what remains in the buffer and then disconnect
  };

  //Status for this connection
  AMQPConnectionState status;

  //The only valid handshake string for AMQP 0-9-1
  //Also the only part which is not specified by the AMQP grammar
  public static final ByteArrayBuffer AMQP_VALID_HANDSHAKE = new ByteArrayBuffer(
    new byte[]{'A', 'M', 'Q', 'P', 0x00, 0x00, 0x09, 0x01}
  );

  //Incoming data queue
  public ByteArrayBuffer queue_incoming = new ByteArrayBuffer();

  //Outgoing data queue
  public ByteArrayBuffer queue_outgoing = new ByteArrayBuffer();

  //The connected peers SocketChannel object
  //Can be used for low level access like shutting down the socket for read/Write
  //access or similar
  public SocketChannel socketChannel;

  //Constructor
  AMQPConnection(SocketChannel socketChannel) {
    //Set initial status to uninitialized/expecting handshake
    status = AMQPConnectionState.UNINITIALIZED;

    //Pointer to the SocketChannel for this client
    this.socketChannel = socketChannel;

    System.out.println("Initialized AMQPConnection");
  }

  //Receive data to be put in the buffer and later decoded
  public void deliverData(byte[] data) {
    queue_incoming.put(data);
  }

  //Receive data directly from a ByteBuffer object
  public void deliverData(ByteBuffer byteBuffer) {
    byte[] data = new byte[byteBuffer.limit()];
    int index = 0;
    while(byteBuffer.hasRemaining()) {
      data[index++] = (byte) byteBuffer.get();
    }
    deliverData(data);
  }

  //Get data which is queued to be sent to the other peer
  public byte[] sendData(int length_limit) {
    return queue_outgoing.get(length_limit);
  }

  //Confirm that length bytes were successfully sent
  //This deletes length bytes of data from the send buffer
  public void confirmSent(int length) {
    queue_outgoing.deleteFront(length);
  }

  //Called when a client disconnects from the server
  public void clientDisconnect(){
    System.out.println("AMQPConnection: Client disconnected");
  }

  //Called whenever a state has been updated, such as when new data has been
  //received or the outgoing queue has changed
  public void updateState() {
    //Is this a newly connected client?
    if (status == AMQPConnectionState.UNINITIALIZED) {
      //If we have enough data for the handshake in the queue, check
      //the handshake signature to make sure this is an AMQP client
      if (queue_incoming.length() >= 8) {
        if (queue_incoming.equals(AMQP_VALID_HANDSHAKE)) {
          System.out.println("Valid handshake received");

          //Handshake is now complete, update our state
          status = AMQPConnectionState.HANDSHAKE_COMPLETE;

          //Remove the handshake from the queue
          queue_incoming.clear();

          //Create Connection.Start programmatically and send it to the peer

          //Create the arguments
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

          //Now waiting for start-ok
          status = AMQPConnectionState.START_SENT;

          //Write to the TCP buffer
          this.queue_outgoing.put(complete_frame.toWire());

        } else {
          //Invalid handshake, write the actual handshake as specified by the documentation
          queue_outgoing.put(AMQP_VALID_HANDSHAKE);

          System.out.println("Received invalid handshake");

          //Clear any remaining data
          queue_incoming.clear();

          //Set the status to DISCONNECT
          status = AMQPConnectionState.DISCONNECT;
        }
      }
    }

    //Are we waiting for CONNECTION.START-OK?
    if (status == AMQPConnectionState.START_SENT && !queue_incoming.empty()) {
      System.out.println("Got data");

      //Make sure at least one full frame has been received before we attempt
      //to decode any data
      if (!AMQPFrame.hasFullFrame(queue_incoming)) {
        System.out.println("Partial frame received");
        return;
      }

      try {
        //This builds the frame object and pops exactly the full frame from
        //the queue_incoming buffer

        //System.out.println(queue_incoming.toHexString());
        AMQPFrame frame = AMQPFrame.build(queue_incoming);
        
        //System.out.println(frame.toWire().toHexString());
      } catch (InvalidFrameException e) {
        System.out.println("InvalidFrameException: " + e.toString());
        //Spec says that any invalid frame should be treated as a fatal error
        //and the connection should be closed without sending more data
        queue_outgoing.clear();
        queue_incoming.clear();

        //Set status to DISCONNECT, causing the server to write any remaining
        //data in the queue and then closing the TCP connection
        status = AMQPConnectionState.DISCONNECT;
      }
    }
  }

  //Returns the bitmask which should be passed into SocketChannel.register()
  public int getSelectorRegisterMask() {
    int value = 0;

    //Always watch for incoming data unless we are in the disconnecting state
    if (this.status != AMQPConnectionState.DISCONNECT) {
      value += SelectionKey.OP_READ;
    }

    //Only watch for write status when we have data in the outgoing queue
    if (this.queue_outgoing.length() > 0) {
      value += SelectionKey.OP_WRITE;
    }
    return value;
  }
}
