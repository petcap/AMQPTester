//Enumerations for the AMQP frame types
public enum AMQPFrameTypeEnum {
  METHOD((byte) 0x01),
  HEADER((byte) 0x02),
  BODY((byte) 0x03),
  HEARTHBEAT((byte) 0x04);

  private byte frameType;

  AMQPFrameTypeEnum(byte frameType) {
    this.frameType = frameType;
  }

  public byte get() {
    return frameType;
  }
};
