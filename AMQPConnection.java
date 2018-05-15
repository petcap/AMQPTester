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
      //the handshake signature and see if it is AMQP valid
      if (queue_incoming.length() >= 8) {
        if (queue_incoming.equals(AMQP_VALID_HANDSHAKE)) {
          System.out.println("Valid handshake received");
          status = AMQPConnectionState.HANDSHAKE_COMPLETE;
          queue_incoming.clear();

          //Temporary CONNECTION.START payload
          //TODO: Build packet object and convert to bytes on the wire
          byte[] tmp_hack_handshake = new byte[] {
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xcf, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xaa, (byte) 0x0c, (byte) 0x63, (byte) 0x61, (byte) 0x70, (byte) 0x61, (byte) 0x62, (byte) 0x69, (byte) 0x6c, (byte) 0x69, (byte) 0x74, (byte) 0x69, (byte) 0x65, (byte) 0x73, (byte) 0x46, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0xb5, (byte) 0x12, (byte) 0x70, (byte) 0x75, (byte) 0x62, (byte) 0x6c, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6e, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6d, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x1a, (byte) 0x65, (byte) 0x78, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67,
            (byte) 0x65, (byte) 0x5f, (byte) 0x65, (byte) 0x78, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x65, (byte) 0x5f, (byte) 0x62, (byte) 0x69, (byte) 0x6e, (byte) 0x64, (byte) 0x69,
            (byte) 0x6e, (byte) 0x67, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x0a, (byte) 0x62, (byte) 0x61, (byte) 0x73, (byte) 0x69, (byte) 0x63, (byte) 0x2e, (byte) 0x6e, (byte) 0x61, (byte) 0x63, (byte) 0x6b,
            (byte) 0x74, (byte) 0x01, (byte) 0x16, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x73, (byte) 0x75, (byte) 0x6d, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x63, (byte) 0x61, (byte) 0x6e, (byte) 0x63,
            (byte) 0x65, (byte) 0x6c, (byte) 0x5f, (byte) 0x6e, (byte) 0x6f, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x79, (byte) 0x74, (byte) 0x01, (byte) 0x12, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x6e,
            (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x2e, (byte) 0x62, (byte) 0x6c, (byte) 0x6f, (byte) 0x63, (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x74, (byte) 0x01,
            (byte) 0x13, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x73, (byte) 0x75, (byte) 0x6d, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6f, (byte) 0x72, (byte) 0x69,
            (byte) 0x74, (byte) 0x69, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x1c, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x69, (byte) 0x63,
            (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x5f, (byte) 0x66, (byte) 0x61, (byte) 0x69, (byte) 0x6c, (byte) 0x75, (byte) 0x72, (byte) 0x65, (byte) 0x5f, (byte) 0x63, (byte) 0x6c,
            (byte) 0x6f, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x01, (byte) 0x10, (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x73, (byte) 0x75, (byte) 0x6d,
            (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x71, (byte) 0x6f, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x0c, (byte) 0x63, (byte) 0x6c, (byte) 0x75, (byte) 0x73, (byte) 0x74, (byte) 0x65, (byte) 0x72,
            (byte) 0x5f, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f, (byte) 0x72, (byte) 0x61, (byte) 0x62, (byte) 0x62, (byte) 0x69, (byte) 0x74,
            (byte) 0x40, (byte) 0x52, (byte) 0x61, (byte) 0x62, (byte) 0x62, (byte) 0x69, (byte) 0x74, (byte) 0x4d, (byte) 0x51, (byte) 0x09, (byte) 0x63, (byte) 0x6f, (byte) 0x70, (byte) 0x79, (byte) 0x72, (byte) 0x69,
            (byte) 0x67, (byte) 0x68, (byte) 0x74, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2e, (byte) 0x43, (byte) 0x6f, (byte) 0x70, (byte) 0x79, (byte) 0x72, (byte) 0x69, (byte) 0x67, (byte) 0x68,
            (byte) 0x74, (byte) 0x20, (byte) 0x28, (byte) 0x43, (byte) 0x29, (byte) 0x20, (byte) 0x32, (byte) 0x30, (byte) 0x30, (byte) 0x37, (byte) 0x2d, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x35, (byte) 0x20,
            (byte) 0x50, (byte) 0x69, (byte) 0x76, (byte) 0x6f, (byte) 0x74, (byte) 0x61, (byte) 0x6c, (byte) 0x20, (byte) 0x53, (byte) 0x6f, (byte) 0x66, (byte) 0x74, (byte) 0x77, (byte) 0x61, (byte) 0x72, (byte) 0x65,
            (byte) 0x2c, (byte) 0x20, (byte) 0x49, (byte) 0x6e, (byte) 0x63, (byte) 0x2e, (byte) 0x0b, (byte) 0x69, (byte) 0x6e, (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x6d, (byte) 0x61, (byte) 0x74, (byte) 0x69,
            (byte) 0x6f, (byte) 0x6e, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x35, (byte) 0x4c, (byte) 0x69, (byte) 0x63, (byte) 0x65, (byte) 0x6e, (byte) 0x73, (byte) 0x65, (byte) 0x64, (byte) 0x20,
            (byte) 0x75, (byte) 0x6e, (byte) 0x64, (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x20, (byte) 0x4d, (byte) 0x50, (byte) 0x4c, (byte) 0x2e, (byte) 0x20, (byte) 0x20,
            (byte) 0x53, (byte) 0x65, (byte) 0x65, (byte) 0x20, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3a, (byte) 0x2f, (byte) 0x2f, (byte) 0x77, (byte) 0x77, (byte) 0x77, (byte) 0x2e, (byte) 0x72,
            (byte) 0x61, (byte) 0x62, (byte) 0x62, (byte) 0x69, (byte) 0x74, (byte) 0x6d, (byte) 0x71, (byte) 0x2e, (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x2f, (byte) 0x08, (byte) 0x70, (byte) 0x6c, (byte) 0x61,
            (byte) 0x74, (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x6d, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x45, (byte) 0x72, (byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x67,
            (byte) 0x2f, (byte) 0x4f, (byte) 0x54, (byte) 0x50, (byte) 0x07, (byte) 0x70, (byte) 0x72, (byte) 0x6f, (byte) 0x64, (byte) 0x75, (byte) 0x63, (byte) 0x74, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x08, (byte) 0x52, (byte) 0x61, (byte) 0x62, (byte) 0x62, (byte) 0x69, (byte) 0x74, (byte) 0x4d, (byte) 0x51, (byte) 0x07, (byte) 0x76, (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6f,
            (byte) 0x6e, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x33, (byte) 0x2e, (byte) 0x35, (byte) 0x2e, (byte) 0x37, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x41,
            (byte) 0x4d, (byte) 0x51, (byte) 0x50, (byte) 0x4c, (byte) 0x41, (byte) 0x49, (byte) 0x4e, (byte) 0x20, (byte) 0x50, (byte) 0x4c, (byte) 0x41, (byte) 0x49, (byte) 0x4e, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x05, (byte) 0x65, (byte) 0x6e, (byte) 0x5f, (byte) 0x55, (byte) 0x53, (byte) 0xce
          };

          System.out.println("Sent CONNECTION.START");
          status = AMQPConnectionState.START_SENT;
          this.queue_outgoing.put(tmp_hack_handshake);

        } else {
          //Invalid handshake, write the actual handshake
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

      //FIXME: Add check that we've actually have a complete frame buffered, not just the beginning
      try {
        ByteArrayBuffer old = queue_incoming.copy();

        System.out.println("Giving this to AMQPFrame.build():");
        System.out.println(queue_incoming.toHexString());

        AMQPFrame frame = AMQPFrame.build(queue_incoming);

        //System.out.println(old.toHexString());
        //System.out.println(queue_incoming.toHexString());

      } catch (InvalidFrameException e) {
        System.out.println("InvalidFrameException: " + e.toString());
        //Spec says that any invalid frame should be treated as a fatal error
        //and the connection should be closed without sending more data
        queue_outgoing.clear();
        queue_incoming.clear();
        status = AMQPConnectionState.DISCONNECT;
      }
    }
  }

  //Returns the bitmask which should be passed into SocketChannel.register()
  public int getSelectorRegisterMask() {
    int value = 0;

    //Always watch for incoming data unless we are disconnecting
    if (this.status != AMQPConnectionState.DISCONNECT) {
      value += SelectionKey.OP_READ;
    }

    //Only attempt to write when we have data in the outgoing queue
    if (this.queue_outgoing.length() > 0) {
      value += SelectionKey.OP_WRITE;
    }
    return value;
  }
}
