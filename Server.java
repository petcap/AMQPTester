import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

  //Listening port for incoming connections
  public static int PORT = 5672;

  //Maximum size of read/write buffers
  public static int SOCKET_BUFFER_SIZE = 1024;

  public static void main(String[] args) {

    //Prevent the compiler from complaining about possibly uninitialized variables
    ServerSocketChannel serverSocketChannel = null;
    Selector selector = null;

    try {
      //Create the NIO socket server and bind it
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.socket().bind(new InetSocketAddress(PORT));

      //The NIO Selector requires nonblocking sockets
      serverSocketChannel.configureBlocking(false);

      //Create a new NIO selector
      selector = Selector.open();
    } catch (IOException e) {
      System.err.println("Failed to create or bind socket:");
      System.err.println(e.toString());
      System.exit(1);
    }

    //Now we've got a listening socket ready and can receive inbound connections
    System.out.println("Listening on port " + PORT);

    try {
      //Register the listening socket in the selector so we'll get notified on
      //incoming connections
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch(ClosedChannelException e) {
      System.err.println("Failed to create selector:");
      System.err.println(e.toString());
      System.exit(1);
    }

    //Master loop that runs when interesting stuff happens
    while(true) {
      try {
        //If select() returns zero, nothing interesting has happened
        //Might be the case if wkaeUp() is called from another thread
        if (selector.select() < 1) {
          continue;
        }

        //Get an interator over all interesting events
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        //Iterate over all interesting channels
        while(keyIterator.hasNext()) {

          //Get the current SelectionKey
          SelectionKey selectionKey = keyIterator.next();

          //Did we receive a new connecting client?
          if (selectionKey.isAcceptable()) {
            //Accept the incoming connection
            SocketChannel sc = ((ServerSocketChannel) selectionKey.channel()).accept();

            //NIO Selector requires non-blocking channels
            sc.configureBlocking(false);

            //Our AMQPConnection which will be responsible for handling this
            //connections
            AMQPConnection amqpConnection = new AMQPConnection(sc);

            //Register the channel with the selector to get notified when
            //interesting events happen, and attach our object to it
            sc.register(selector, amqpConnection.getSelectorRegisterMask(), amqpConnection);

            //Print out the connection info
            SocketAddress socketAddress = sc.getRemoteAddress();
            if (socketAddress != null) {
              System.out.println("Accepted connection from " + socketAddress.toString());
            }
          }

          //Did a socket get connected?
          if (selectionKey.isConnectable()) {
            //Currently not implemented as the project never initiates outgoing connections
          }

          //Did we receive some data?
          if (selectionKey.isReadable()) {
            //Get the AMQPConnection associated with this connection
            AMQPConnection amqpConnection = (AMQPConnection) selectionKey.attachment();

            //Get the SocketChannel which received data
            SocketChannel readSocketChannel = (SocketChannel) selectionKey.channel();

            //Allocate a ByteBuffer
            ByteBuffer readByteBuffer = ByteBuffer.allocate(SOCKET_BUFFER_SIZE);

            //Attempt to read data into the byte buffer
            //If the read() method returns -1, we have a connection error
            if (readSocketChannel.read(readByteBuffer) == -1) {
              keyIterator.remove();
              readSocketChannel.close();
              amqpConnection.clientDisconnect();
              continue;
            }

            //Flip the buffer into reading mode
            readByteBuffer.flip();

            //Deliver the protocol data to the AMQPConnection buffer
            amqpConnection.deliverData(readByteBuffer);

            //Notify the AMQPConnection object that we have an updated state
            amqpConnection.updateState();

            //Update the selector interest set
            readSocketChannel.register(selector, amqpConnection.getSelectorRegisterMask(), amqpConnection);
          }

          //Are we ready to write data?
          if (selectionKey.isWritable()) {
            //Get the AMQPConnection associated with this connections
            AMQPConnection amqpConnection = (AMQPConnection) selectionKey.attachment();

            //Check that we actually have data to write
            //This check should never fail
            if (amqpConnection.queue_outgoing.length() == 0) {
              System.err.println("Attemping to write data to channel without any actual data to write");
              System.exit(1);
            }

            //Get the SocketChannel which received data
            SocketChannel writeSocketChannel = (SocketChannel) selectionKey.channel();

            //Allocate a write ByteBuffer
            ByteBuffer write = ByteBuffer.allocate(SOCKET_BUFFER_SIZE);

            //Populate the write buffer with pending outgoing protocol data
            write.put(amqpConnection.sendData(SOCKET_BUFFER_SIZE));

            //Flip the ByteBuffer object to read mode...
            write.flip();

            //...and attempt to write data to the socket
            int writeStatus = writeSocketChannel.write(write);

            //Did the send fail?
            if (writeStatus == -1) {
              System.out.println("Lost client when attempting to send data");
              writeSocketChannel.close();
              amqpConnection.clientDisconnect();
              continue;
            }

            //Tell the object how much data has been sent via the socket
            amqpConnection.confirmSent(writeStatus);

            //Check if the client should be disconnected
            if (amqpConnection.queue_outgoing.length() == 0 && amqpConnection.status == AMQPConnection.AMQPConnectionState.DISCONNECT) {
              writeSocketChannel.close();
              System.out.println("Disconnected a client");
              continue;
            }

            //Notify the AMQPConnection object that we have an updated state
            amqpConnection.updateState();

            //Update the selector interest set
            writeSocketChannel.register(selector, amqpConnection.getSelectorRegisterMask(), amqpConnection);
          }

          keyIterator.remove();
        }
      } catch(IOException e) {
        System.err.println("select() failure:");
        System.err.println(e.toString());
      }
    }
  }
}
