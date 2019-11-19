/*
* This class represents a byte array buffer which is used to queue read and
* write data which is to be sent over the network
*/

import java.util.Arrays;
import java.nio.*;

public class ByteArrayBuffer {
  public byte[] buffer;

  //Constructor
  ByteArrayBuffer() {
    buffer = new byte[0];
  }

  //Constructor
  ByteArrayBuffer(byte[] arr) {
    buffer = arr.clone();
  }

  //Constructor
  ByteArrayBuffer(byte b) {
    buffer = new byte[]{b};
  }

  //Constructor
  ByteArrayBuffer(String string) {
    buffer = string.getBytes();
  }

  //Build one ByteArrayBuffer from many smaller ones
  public static ByteArrayBuffer build(ByteArrayBuffer... buffers) {
    ByteArrayBuffer ret = new ByteArrayBuffer();
    for (ByteArrayBuffer c : buffers) {
      ret.put(c);
    }
    return ret;
  }

  //Put a byte[] at the end of the queue
  public void put(byte[] arr) {
    byte[] new_buffer = new byte[buffer.length + arr.length];
    System.arraycopy(buffer, 0, new_buffer, 0, buffer.length);
    System.arraycopy(arr, 0, new_buffer, buffer.length, arr.length);
    buffer = new_buffer;
  }

  //Put a ByteArrayBuffer at the end of the queue
  public void put(ByteArrayBuffer byteArrayBuffer) {
    this.put(byteArrayBuffer.get());
  }

  //Put a String at the end of the queue
  public void put(String string) {
    put(string.getBytes());
  }

  //Clear the buffer
  public void clear() {
    buffer = new byte[0];
  }

  //Delete no bytes from the beginning of the queue
  public void deleteFront(int no) {
    byte[] new_buffer = new byte[buffer.length - no];
    System.arraycopy(buffer, no, new_buffer, 0, buffer.length - no);
    buffer = new_buffer;
  }

  //Get the entire buffer as a byte array
  public byte[] get() {
    return buffer;
  }

  public boolean empty() {
    return this.length() == 0;
  }

  //Get the first `limit` bytes of the queue
  public byte[] get(int limit) {
    return get(0, limit);
  }

  //Get a specific byte
  public byte getByte(int offset) {
    return buffer[offset];
  }

  //Get the last limit bytes of the queue
  public byte[] getEnd(int limit) {
    if (limit > buffer.length) return buffer;
    return Arrays.copyOfRange(buffer, buffer.length-limit, buffer.length);
  }

  //Make a copy of this object
  public ByteArrayBuffer copy() {
    return new ByteArrayBuffer(buffer);
  }

  //Pops the first limit bytes, returning them as a separate byteArrayBuffer
  //and deleting them from this object
  public ByteArrayBuffer pop(int limit) {
    if (limit > buffer.length) limit = buffer.length;
    ByteArrayBuffer ret = new ByteArrayBuffer(this.get(limit));
    this.deleteFront(limit);
    return ret;
  }

  //Pops the first limit bytes, returning them as a separate byteArrayBuffer
  //and deleting them from this object
  public ByteArrayBuffer pop(long limit) {
    return pop((int) limit);
  }

  //Get buffer between two offsets
  public byte[] get(int start, int stop) {
    if (start > buffer.length || start < 0) start = 0;
    if (stop > buffer.length || stop < 0) stop = buffer.length;
    return Arrays.copyOfRange(buffer, start, stop);
  }

  //Get buffer between two offsets as a ByteArrayBuffer
  public ByteArrayBuffer getByteArrayBuffer(int start, int stop) {
    if (start > buffer.length || start < 0) start = 0;
    if (stop > buffer.length || stop < 0) stop = buffer.length;
    return new ByteArrayBuffer(Arrays.copyOfRange(buffer, start, stop));
  }

  //Get the length of the buffer
  public int length() {
    return buffer.length;
  }

  //Get the buffer as a String
  public String toString() {
    return new String(buffer);
  }

  //Check if this object is identical to another
  public boolean equals(ByteArrayBuffer cmp) {
    return (Arrays.equals(cmp.get(), buffer));
  }

  //Check if this object is identical to a byte array
  public boolean equals(byte[] cmp) {
    return (Arrays.equals(cmp, buffer));
  }

  /*
  This method is originally taken from
  https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java#9855338
  */
  //Get the buffer as a hexadecimal encoded string for debugging purposes
  public String toHexString() {
    byte[] hexArray = "0123456789abcdef".getBytes();
    byte[] hexChars = new byte[buffer.length * 2];
    for (int j = 0; j < buffer.length; j++) {
      int v = buffer[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return "(frame below is of length " + buffer.length + ")\n" + new String(hexChars).replaceAll("(.{2})", "$1 ");
  }

  /*
  This method is originally taken from
  https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java#9855338
  */
  //Get the buffer as a hexadecimal encoded string for debugging purposes
  public static String byteArrayToString(byte[] in) {
    byte[] hexArray = "0123456789abcdef".getBytes();
    byte[] hexChars = new byte[in.length * 2];
    for (int j = 0; j < in.length; j++) {
      int v = in[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String byteArrayToString(ByteArrayBuffer in) {
    return byteArrayToString(in.get());
  }

  public long toLong() {
    return bytesToLong(this.buffer);
  }

  public int toInt() {
    return bytesToInt(this.buffer);
  }

  //Taken from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
  public static byte[] longToBytes(long l) {
      byte[] result = new byte[8];
      for (int i = 7; i >= 0; i--) {
          result[i] = (byte)(l & 0xFF);
          l >>= 8;
      }
      return result;
  }

  //Return buffer size
  public int size() {
    return buffer.length;
  }

  //Taken (and modified) from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
  public static int bytesToInt(byte[] b) {
      int result = 0;
      for (int i = 0; i < b.length; i++) {
          result <<= 8;
          result |= (b[i] & 0xFF);
      }
      return result;
  }

  //Taken (and modified) from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
  public static long bytesToLong(byte[] b) {
      long result = 0;
      for (int i = 0; i < b.length; i++) {
          result <<= 8;
          result |= (b[i] & 0xFF);
      }
      return result;
  }
};
