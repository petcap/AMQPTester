//This class is used to collect errors from the rest of the code when testing
//a client

import java.io.*;

public class AMQPErrorCollector {
  static PrintWriter printWriter;

  public static void log(String message) {
    //Initialize the PrintWriter
    if (printWriter == null) {
      try {
        printWriter = new PrintWriter(new FileWriter("/tmp/amqptester.txt"));
      } catch (IOException e) {
        System.out.println("Unable to open /tmp/amqptester.txt (fatal error)");
        System.out.println(e.toString());
        System.exit(1);
      }
    }

    //Write to the log
    printWriter.write(message + "\n");
  }

};
