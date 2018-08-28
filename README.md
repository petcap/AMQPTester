# AMQPTester
An AMQ Protocol tester and validator written in Java.

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

This is academic code, expect things to break. The code is not very well optimized, as a lot of buffers are copied back and forth a lot. This makes the code slower than other Java implementations of AMQP.
