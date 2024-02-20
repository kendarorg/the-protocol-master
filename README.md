## The Protocol Master

The "protocol-master" is a project aimed at various tasks

* Create a state machine able to interpret a generic wire protocol, handling
  special situations like incomplete messages, sanding and receiving data according
  to byte ordering and formatting
* Translate the queries in a standard, serializable format, logging all queries
  and results in a consistent way
* Forward wire protocol to drivers. For SQL means passing the queries to JDBC drivers,
  for NO-SQL forwarding to the specific ones like MongoDB or Redis
* Run queries against a pre-recorded sequence of commands to simulate a real data
  storage, without the need of a real server (in the making)

For this to become real an event based state machine has been developed, with
several database wire protocol implementations:

* [PostgresSQL](protocol-postgres/README.md) Usable for most db (for hibernate you should set the final db dialect of
  course)
    * Support for simple and extended query protocol
    * Transactions
    * Simple authentication (could add an "auth provider")
* [MySQL](protocol-mysql/README.md)
    * Support for cached queries and stored procedures
    * Simple authentication (could add an "auth provider")
* [MongoDB](protocol-mongo/README.md)
* Redis (soon)
* ActiveMQ (soon)

## Using it out of the box

You can use the "protocol-runner-VERSION.jar" to proxy all your calls and test your
connections (and add some ISSUE to this project hopefully).

Inside protocol-runner/src/test/java/org/kenndar/runner/MainTest.java you can see
an example where a recording is made and then reporduced.

Just call it like the following:

<pre>
  java -jar protocol-runner.jar \
    -p postgres -l 3175 \
    -xl remoteUser -xw remotePassword -xc jdbc:postgresql://remoteDb/test \
    -xd test/{timestamp}
</pre>

<pre>
usage: runner
 -l <arg>    Select listening port
 -p <arg>    Select protocol (mysql/mongo/postgres
 -pl         Replay from log directory
 -xc <arg>   Select remote connection string
 -xd <arg>   Select remote log directory (you can set a {timestamp} value
             that will be replaced with the current timestamp)
 -xl <arg>   Select remote login
 -xw <arg>   Select remote password
</pre>

Inside the choosen directory you will find simple jsons containing all the data exchanged
with the server AND you can modify it before replaying, to simulate special situations!

## The state machine

### TLDR

The state machine (or better the Turing Machine, having memory) is based on

* States (extending ProtoState) able to interact with events
* Events (extending BaseEvent) to invoke action on states
* Context (extending ProtoContext) it is the memory storage for the current
  execution (connection) and the current state

### Execution

When bytes arrives to the TM they are transformed in a "BytesEvent" and then
consumed by the specific connection thread, then all the child states of the current
state are verified. They have the responsibility to check if the message is of the
correct type and then if the content is matching the signature.

When a state is hit it can send events or response messages. When an execution runs
without errors then the "executed incoming buffer" is purged from the received bytes

The events are immediatly executed. If no bytes are present or there are not enough
bytes to read, the events queue is seeked for "BytesEvent" and then the execution is
retried.

