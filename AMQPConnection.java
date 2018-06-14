/*
* This class represents a connected AMQP client.
*/

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class AMQPConnection {

  //Valid states for a connection
  public static enum AMQPConnectionState {
    UNINITIALIZED,        //Default state
    HANDSHAKE_COMPLETE,   //After handshake is complete
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

  //Higher level tester objects to which full AMQP frames are delivered to and
  //read from
  public AMQPTester tester;

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

  //Periodically called (currently every 1 sec) by the server code
  public void periodical() {
    tester.periodical();
    System.out.println(queue_outgoing.toHexString());
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
    //Is this a newly connected client? In that case, wait for a handshake
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

          //Initialize the tester
          //FIXME: Initialize different testers in the future depending on
          //command line arguments we receive
          tester = new AMQPTesterSimple(this);

          //The connection is now initialized and ready to be taken over by the AMQPTester
          status = AMQPConnectionState.HANDSHAKE_COMPLETE;

          //Call this method again since we've got an updated state
          updateState();
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

    //Is the handshake complete?
    if (status == AMQPConnectionState.HANDSHAKE_COMPLETE) {
      //While we have one or more frames buffered...
      while(AMQPFrame.hasFullFrame(queue_incoming)) {

        try {
          //Build a frame object from the buffer
          AMQPFrame frame = AMQPFrame.build(queue_incoming);

          //Deliver the frame object to the tester
          tester.deliverFrame(frame);

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

      //The tester (maybe) wants to deliver a frame back to the client
      AMQPFrame reply = tester.getFrame();

      //Read all packets from the tester and convert them to low level frames
      //and queue them up in the TCP buffer
      while (reply != null) {
        this.queue_outgoing.put(reply.toWire());
        reply = tester.getFrame();
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
