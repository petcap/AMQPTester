/*
 * Exception for invalid frame decodings
 */
public class InvalidFrameException extends Exception {
  public InvalidFrameException(String message) {
    super(message);
  }
}
