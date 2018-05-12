//Enumerations for the AMQP connection status
public enum AMQPConnectionStatusEnum {
  UNINITIALIZED,        //Default state
  HANDSHAKE_COMPLETE,   //After handshake is complete
  START_SENT,   //After handshake is complete
  DISCONNECT            //When the server code should write what remains in the buffer and then disconnect
};
