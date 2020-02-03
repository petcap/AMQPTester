# AMQPTester
An AMQP Protocol tester and validator written in Java. Implements AMQP from scratch using the Java NIO socket API. Tested with a variety of clients such as Py-AMQP, RabbitMQ-Java, AMQP-PHP and others.

This code was developed for my masters thesis at The Royal Institute of Technology (KTH) in Stockholm, Sweden. It implements the AMQP 0-9-1 wire-level protocol as described by the formal specification. For a more in-depth explanation of the code, please see the thesis itself.

Compile and run using:
```
$ javac *.java
$ java Server
```

Use a specific test by running:
```
$ java Server test-name
```

Example: To run the multiple channels test, execute:
```
$ java Server channels
```

This is academic code and is hence not meant to be used in production environments. All code in this repository is released under GNU GPLv2.
