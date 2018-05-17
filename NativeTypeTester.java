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
      test = new ByteArrayBuffer(new byte[]{
        0x00, 0x00, 0x00, 0x00, //Length, updated below for convenience
        'S', //First member type
        0x00, 0x00, 0x00, 0x04, 'A', 'M', 'Q', 'P', //First member
      });
      //Update Field Table length
      test.buffer[3] = (byte) ((test.buffer.length) - 4);

      System.out.println("From wire: " + test.toHexString());
      System.out.println("To wire  : " + new AFieldTable(test).toWire().toHexString());
      System.out.println("-----------------------------------");

    } catch (InvalidTypeException e) {
      System.out.println("Caught type exception: " + e.toString());
    }
  }
};
