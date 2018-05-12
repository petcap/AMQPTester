/*
* This class represents an AMQP table
*/

public class AMQPTable {
  public ByteArrayBuffer[] table = new ByteArrayBuffer[0];

  //Constructor
  AMQPTable() {

  }

  //Put a ByteArrayBuffer at the end of the table
  public void put(ByteArrayBuffer byteArrayBuffer) {
    ByteArrayBuffer[] new_table = new ByteArrayBuffer[table.length + 1];
    for(int i=0; i!=table.length; ++i) {
      new_table[i] = table[i];
    }
    new_table[table.length] = byteArrayBuffer;
    table = new_table;
  }

  //Returns the length of the table
  public int length() {
    return table.length;
  }

  //Returns the ByteArrayBuffer which is on top of the table and deletes it
  //Returns an empty ByteArrayBuffer if the table is empty
  public ByteArrayBuffer pop() {
    if (table.length == 0) return new ByteArrayBuffer("");
    ByteArrayBuffer ret = table[0];
    ByteArrayBuffer[] new_table = new ByteArrayBuffer[table.length - 1];
    for(int i=0; i!=new_table.length; ++i) {
      new_table[i] = table[i+1];
    }
    table = new_table;
    return ret;
  }
}
