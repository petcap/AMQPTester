//Class represeting a tester, this class is implemented depending
//on what sort of behaviour we want to test
//Once a client has connected and presented a valid handshake, this class is
//instantiated and takes over the more high level parts of sending and receiving
//AMQPFrames

public class AMQPTester {
  //Deliver a frame received over the wire
  public void deliverFrame(AMQPFrame amqpFrame){}

  //Get a frame from the tester which is to be sent over the wire
  //Returns null when there are no frames to be delivered
  public AMQPFrame getFrame(){
    return null;
  }
};
