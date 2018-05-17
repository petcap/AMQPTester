//Decode data and then reencode the same data and compare of we get the same result
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class NativeTypeTester {
  public static void main(String[] args) {
    try {
      System.out.println("Starting tests...");
      ByteArrayBuffer test;

      System.out.println("ABoolean:");
      test = new ByteArrayBuffer(new byte[]{ 0x02 });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new ABoolean(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("ABoolean:");
      test = new ByteArrayBuffer(new byte[]{ 0x00 });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new ABoolean(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("ALongLongUInt:");
      test = new ByteArrayBuffer(new byte[]{ (byte) 0xff, 0x00, 0x13, 0x37, 0x22, 0x33, 0x44, 0x55 });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new ALongLongUInt(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("ALongUInt:");
      test = new ByteArrayBuffer(new byte[]{ (byte) 0xff, 0x13, 0x37, 0x55 });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new ALongUInt(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("AShortString:");
      test = new ByteArrayBuffer(new byte[]{ (byte) 0x04, 'A', 'M', 'Q', 'P' });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new AShortString(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("ALongString:");
      test = new ByteArrayBuffer(new byte[]{ 0x00, 0x00, 0x00, 0x04, 'A', 'M', 'Q', 'P' });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new ALongString(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("AOctet:");
      test = new ByteArrayBuffer(new byte[]{ 0x37 });
      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new AOctet(test).toWire().toHexString());
      System.out.println("-----------------------------------");

      System.out.println("AFieldTable:");

      //Test frame contents
      //This is a complete nested Field Table
      //test = new ByteArrayBuffer(new byte[]{
      //  0x00, 0x00, 0x00, 0x00, //Length, updated below for convenience
      //  0x04, 'T', 'e', 's', 't', //First key name, short string
      //  'S', //First member type, long string
      //  0x00, 0x00, 0x00, 0x04, 'A', 'M', 'Q', 'P', //First member
      //  0x05, 'T', 'e', 's', 't', '2', //Second key name, short string
      //  't', //Second member type, boolean
      //  0x00, //Second member
      //  0x05, 'T', 'e', 's', 't', '3', //Third key name, short string
      //  'F', //Third member type, nested field table
      //    0x00, 0x00, 0x00, 0x0e, //Nested field table length
      //    0x06, 'N', 'e', 's', 't', 'e', 'd', //Nested key name
      //    's', //First nested type, short string
      //    0x05, //Nested string length
      //    'H', 'e', 'l', 'l', 'o' //Nested string payoad
      //});

      ////Update Field Table length
      //test.buffer[3] = (byte) ((test.buffer.length) - 4);

      ////Build a new Method Frame
      //AMQPInnerFrame methodFrame = new AMQPMethodFrame(
      //  new AShortUInt(10),
      //  new AShortUInt(11),
      //  test.copy() //Give copy of arguments
      //);

      //System.out.println("Argument list: " + test.toHexString());
      //System.out.println("Frame data: " + methodFrame.toWire().toHexString());
      //System.out.println(new AFieldTable(test.copy()).toString());
      //System.out.println("-----------------------------------");

      //Actual argument list from phpamqp library
      //This fails for some reason
      test = new ByteArrayBuffer(new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf3, (byte) 0x07, (byte) 0x70, (byte) 0x72, (byte) 0x6f, (byte) 0x64, (byte) 0x75, (byte) 0x63, (byte) 0x74, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x41, (byte) 0x4d, (byte) 0x51, (byte) 0x50, (byte) 0x4c, (byte) 0x69, (byte) 0x62, (byte) 0x08, (byte) 0x70, (byte) 0x6c, (byte) 0x61, (byte) 0x74, (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x6d, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x50, (byte) 0x48, (byte) 0x50, (byte) 0x07, (byte) 0x76, (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x32, (byte) 0x2e, (byte) 0x36, (byte) 0x0b, (byte) 0x69, (byte) 0x6e, (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x6d, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x63, (byte) 0x6f, (byte) 0x70, (byte) 0x79, (byte) 0x72, (byte) 0x69, (byte) 0x67, (byte) 0x68, (byte) 0x74, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x63, (byte) 0x61, (byte) 0x70, (byte) 0x61, (byte) 0x62, (byte) 0x69, (byte) 0x6c, (byte) 0x69, (byte) 0x74, (byte) 0x69, (byte) 0x65, (byte) 0x73, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x8c, (byte) 0x1c, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x69, (byte) 0x63, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x5f, (byte) 0x66, (byte) 0x61, (byte) 0x69, (byte) 0x6c, (byte) 0x75, (byte) 0x72, (byte) 0x65, (byte) 0x5f, (byte) 0x63, (byte) 0x6c, (byte) 0x6f, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x01, (byte) 0x12, (byte) 0x70, (byte) 0x75, (byte) 0x62, (byte) 0x6c, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6d, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x16, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x73, (byte) 0x75, (byte) 0x6d, (byte) 0x65, (byte) 0x72, (byte) 0x5f, (byte) 0x63, (byte) 0x61, (byte) 0x6e, (byte) 0x63, (byte) 0x65, (byte) 0x6c, (byte) 0x5f, (byte) 0x6e, (byte) 0x6f, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x79, (byte) 0x74, (byte) 0x01, (byte) 0x1a, (byte) 0x65, (byte) 0x78, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x65, (byte) 0x5f, (byte) 0x65, (byte) 0x78, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x65, (byte) 0x5f, (byte) 0x62, (byte) 0x69, (byte) 0x6e, (byte) 0x64, (byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x73, (byte) 0x74, (byte) 0x01, (byte) 0x0a, (byte) 0x62, (byte) 0x61, (byte) 0x73, (byte) 0x69, (byte) 0x63, (byte) 0x2e, (byte) 0x6e, (byte) 0x61, (byte) 0x63, (byte) 0x6b, (byte) 0x74, (byte) 0x01, (byte) 0x12, (byte) 0x63, (byte) 0x6f, (byte) 0x6e, (byte) 0x6e, (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e, (byte) 0x2e, (byte) 0x62, (byte) 0x6c, (byte) 0x6f, (byte) 0x63, (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x74, (byte) 0x01, (byte) 0x08, (byte) 0x41, (byte) 0x4d, (byte) 0x51, (byte) 0x50, (byte) 0x4c, (byte) 0x41, (byte) 0x49, (byte) 0x4e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x23, (byte) 0x05, (byte) 0x4c, (byte) 0x4f, (byte) 0x47, (byte) 0x49, (byte) 0x4e, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x67, (byte) 0x75, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x08, (byte) 0x50, (byte) 0x41, (byte) 0x53, (byte) 0x53, (byte) 0x57, (byte) 0x4f, (byte) 0x52, (byte) 0x44, (byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x67, (byte) 0x75, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x05, (byte) 0x65, (byte) 0x6e, (byte) 0x5f, (byte) 0x55, (byte) 0x53
      });

      System.out.println(test.toHexString());
      AFieldTable field = new AFieldTable(test);
      System.out.println(field.toWire().toHexString());

    } catch (Exception e) {
      System.out.println("Caught exception: " + e.toString());
    }
  }
};
