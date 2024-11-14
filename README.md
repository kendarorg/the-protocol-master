## The Protocol Master

![](protocolmaster_s.gif)

The "protocol-master" is a java command line tool to emulate/change/record
APIs and server calls for testing apps with the following protocol: 
HTTP, HTTPS, Postgres, MySQL, Redis, Mqtt, RabbitMQ, AMQP 091
... and all the compatible ones!

The main targets are testing and inspection

You can use it to:

* Block troubles BEFORE PRODUCTION
* Simulate errors
* Simulate behaviours
* Mock responses without coding
* Mock entire infrastructures without coding
* "Gold standard" test without infrastructure without coding

The whole project is covered with Jacoco (73% coverage actually)

## Get Started

Start looking at [tutorials](docs/tutorials.md) for a good start

## Examples

Open a http/s proxy on port 9999 and record everything that goes through

```
java -jar protocol-runner.jar -protocol http -proxy 9999 \
    -record
```

Create a local postgres "forward proxy" to a real one and record everything

```
java -jar protocol-runner.jar -protocol postgres  \
    -connection jdbc:postgresql://REALSERVER:5432  \
    -login REALLOGIN -password REALPWD \
    -record
```

Open a http/s proxy on port 9999 and return a 503 error, on 50% of calls

```
java -jar protocol-runner.jar -protocol http -proxy 9999 \
    -showError 503 -errorPercent 40
```

## Features

* Language independent: use the native protocols without drivers

* Run on all OSs
  * Windows
  * Linux
  * macOs

* Native protocol for (and all compatibles):
  * anything that goes through http/https
  * postgres
  * mysql
  * mongoDB
  * redis 3,redis 2
  * mqtt
  * rabbitmq/Amqp 0.9
  

* Mocked errors (http/s)
* Mocked responses (all)
* Support gzip,brotli,jacksonSmile,multipart and more (http/s)
* Mocked error response (http/s)
* Internal rewrite queries and urls (http/s,mysql,postgres)
* Record activity (all)
* Replay activity (all)
* Custom transparent proxy (all)
* Translate postgres to any Jdbc supported db (like Oracle!)










* Create a state machine able to interpret a generic wire protocol, handling
  special situations like incomplete messages, sanding and receiving data according
  to byte ordering and formatting
* Translate the queries in a standard, serializable format, logging all queries
  and results in a consistent way
* Forward wire protocol to drivers. For SQL means passing the queries to JDBC drivers,
  for NO-SQL forwarding to the specific ones like MongoDB or AMQP
* Run queries against a pre-recorded sequence of commands to simulate a real data
  storage, without the need of a real server (in the making)
* Adding custom Java plugins

The whole project is covered with Jacoco (66% coverage actually)

For this to become real an event based state machine has been developed, with
several database wire protocol implementations:

* [Http](protocol-http/README.md)
    * Support Http and Https mitm
    * Simulate errors for chaos engineering
    * Simulate API behaviours
    * Mock responses
    * Record everything
* [PostgresSQL](protocol-postgres/README.md)  Usable for most db (for hibernate you should set the final db dialect of
  course)
    * Support for simple and extended query protocol
    * Transactions
    * Simple authentication (could add an "auth provider")
* [MySQL](protocol-mysql/README.md)
    * Support for cached queries and stored procedures
    * Simple authentication (could add an "auth provider")
* [MongoDB](protocol-mongo/README.md)
    * Basic authentication
* [RabbitMq/AMQP 0.9.1](protocol-amqp-091/README.md)
    * Support for basic queue/publish/consume
    * Channels multiplexing
* [Redis](protocol-redis/README.md)
    * Support for subscriptions
    * Support for push parsing
    * RESP2 and RESP3 supported out of the box
* [Mqtt](protocol-mqtt/README.md)
    * Support QOS 1,2,3
    * Support 3.x and 5.x protocols

## If you like it Buy me a coffe :)

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/paypalme/kendarorg/1)

## Using it out of the box

* Inside directory "recording"
* For protocol "http" (and https)
* Record all request response

```
java -jar protocol-runner.jar -protocol http -record
```

* Inside directory "recording"
* For protocol postgres
* Fake a postgres db listening on 7715
* Forward requests to remotedb:5432
* With login reallogin and password realpassword
* Record all query and responses

```
java -jar protocol-runner.jar -datadir recording -protocol postgres -port 7715 \
  -connection jdbc:postgresql://remotedb:5432 -login reallogin -password realpassword \
  -record
```

You can use the "protocol-runner-VERSION.jar" to proxy all your calls and test your
connections (and add some ISSUE to this project hopefully).

Inside protocol-runner/src/test/java/org/kendar/runner/MainTest.java you can see
an example where a recording is made and then reporduced.

Just call it like the following if using included mysql and postgres driver (adapt
the line end to your system!):
<pre>
  java -jar protocol-runner.jar \
    -p postgres -l 3175 \
    -xl remoteUser -xw remotePassword -xc jdbc:postgresql://remoteDb/test \
    -xd test/{timestamp}
</pre>

Or like this to use an external driver (in this case Oracle JDBC driver) (adapt
the cp/classpath and line end to your system!)

<pre>
java -cp "ojdbc11.jar;protocol-runner.jar" \
    org.springframework.boot.loader.JarLauncher \
    -p postgres -l 5432 \
    -xl system -xw password -xc jdbc:oracle:thin:@192.168.1.96:1521/FREEPDB1 \
    -xd test/{timestamp}
</pre>

<pre>
usage: runner
 -l <arg>    [all] Select listening port
 -p <arg>    Select protocol (mysql/mongo/postgres/amqp091/redis/mqtt)
 -pl         [all] Replay from log/replay directory
 -t <arg>    [all] Set timeout in seconds towards proxied system (default
             30s)
 -v <arg>    [all] Log level (default ERROR)
 -xc <arg>   [all] Select remote connection string (for redis use
             redis://host:port
 -xd <arg>   [all] Select log/replay directory (you can set a {timestamp}
             value
             that will be replaced with the current timestamp)
 -xl <arg>   [mysql/mongo/postgres/amqp091/mqtt] Select remote login
 -xw <arg>   [mysql/mongo/postgres/amqp091/mqtt] Select remote password
 -jr <arg>   [jdbc] Replace queries
 -js <arg>   [jdbc] Set schema
</pre>

Inside the chosen directory you will find simple jsons containing all the data exchanged
with the server AND you can modify it before replaying, to simulate special situations!

### Set Schema

The set schema is called in case the jdbc driver does not allow setting the schema from connection string

### Replace Queries

Specify a file containing "replacement queries" this is specially useful when running ... the runner
as postgres and contacting a different kind of database. Here can be inserted the replacements.

SPACE ARE IMPORTANT INSIDE THE QUERY. THEY MUST MATCH THE REAL ONE.
AND NO ";" SHOULD BE ADDED AT THE END OF QUERIES

This first example replaces "SELECT 1 AS TEST" directly with "SELECT 2 AS TEST".

<pre>
#find
SELECT 
 1 AS TEST
#replace
SELECT 
 2 AS TEST
</pre>

This second example replaces "SELECT anynumber AS TEST" with "SELECT anynumber+1 AS TEST"
So if you send a "SELECT 10 AS TEST" the resultset will contain a 12.

Please notice the usage of the $1 in the capture group.

<pre>
#regexfind
SELECT 
 ([0-9]+) AS TEST
#replace
SELECT 
 $1+2 AS TEST
</pre>

This third example replaces "SELECT anynumber AS TEST" directly with "SELECT 2 AS TEST".

In this case the capture group is not used and the whole query will be ALWAYS be replaced

<pre>
#regexfind
SELECT 
 ([0-9]+) AS TEST
#replace
SELECT 
 2 AS TEST
</pre>

## The state machine-Behaviour tree

### TLDR

The state machine (or better the Behaviour tree fsm) is based on

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

## Suggestion for run and compilation

You can build the whole system at home, but to avoid messing with timeouts

* Maven clean & install each project separately
* Run the jacoco to check the coverage



