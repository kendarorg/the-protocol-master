## The Protocol Master

![](protocolmaster_s.gif)

"The Protocol Master" is an infrastructure simulator to test effortlessy your app in minutes (with no coding) supporting
HTTP, HTTPS, Postgres, MySQL, Redis, Mqtt, RabbitMQ, AMQP 091 ... and all the compatible ones!

* Simulate wrong scenarios
* Block troubles before production
* Simulate whole infrastructure
* Make untestable apps testable
* Easy Chaos engineering

Effortlessy and with zero budget

### Get Started

Look at "[The Protocol Master Samples](https://github.com/kendarorg/the-protocol-master-samples)" repository to create
a complete docker environment to have a gimplse of the features

### How it works

It's simple it's a multiprotocol proxy on your box (or wetheaver you want)
Independent from the stack you are using

### Scenarios

* Simulate what-if without hitting real servers
* Testing resiliency, errors and wrong data
* Security analisys (what goes on the wire)
* Test new APIs easily
* Understand all consequences of changes

### Custom Plugins

You can build your own plugins, just place the Jar into the plugins dir
and intercept all it's intercepted by TPM. Add headers, change data,
simulate specific needs

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
* Mocked flow (all)
* Support gzip,brotli,jacksonSmile,multipart and more (http/s)
* Mocked error response (http/s)
* Mocked (optionally parametrized) responses (http/s,mysql,postgres) (nth, and count)
* Internal rewrite queries and urls (http/s,mysql,postgres)
* Record/replay activity-Gold standards-Zero infrastructure (all)
* Handle callback mocks during replay automagically (mqtt,amqp,redis)
* Custom transparent proxy (all)
* Translate postgres and MySQL to any Jdbc supported db like Oracle!
* Plugin-based architecture
* Custom maven repository on [https://maven.kendar.org](https://maven.kendar.org/maven2/releases/org/kendar/protocol/)

The configuration is based on command line parameters or a json properties file
for the usage check [here](docs/properties.md)

For the api usage [here](docs/apis.md)

If you want to go on the specific functions by protocol:

* [Http](protocol-http/README.md)
    * Support Http and Https as Man In The Middle
    * Custom proxy to intercept without DNS changes
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
    * Support for all kind of publish/consume
    * Channels multiplexing
* [Redis](protocol-redis/README.md)
    * Support for subscriptions
    * Support for push parsing
    * RESP2 and RESP3 supported out of the box
* [Mqtt](protocol-mqtt/README.md)
    * Support QOS 1,2,3
    * Support 3.x and 5.x protocols

## How it was Born

I had an always missing QA environment and all changes should be checked by legal (it was in Electronic ID field). I
needed to simulate APIs, Database calls, and full scenarios, from UI to the DB.

I started developing a series of docker container in PHP to intercept all incoming and outgoing HTTP/S call, with DNS
server and certificates, you can find it [here](https://github.com/kendarorg/HttpAnsweringMachine.php).

Becoming the thing too hard to cope with i started the development of a Java integrated version with fancy UI and all
integrated service, the [HttpAnsweringMachine](https://github.com/kendarorg/HttpAnsweringMachine) based on Spring-Boot
and leveraging a custom routing system, dns server and so on.

But that was too really hard to interact with. I started developing a JDBC->HTTP->JDBC
driver, [Janus-Jdbc](https://github.com/kendarorg/janus-jdbc) from scratch, then started creating a similar thing for
.NET, thing went rogues in a short time with [Janus-Ado](https://github.com/kendarorg/janus-ado). But i started
understanding Postgres protocol.

I started then with a command line utility to parse and translate binary protocols and that's how "The Protocol Master"
was born. Always with a plugin-based architecture. Then i started adapting all the knowledge of HttpAnsweringMachine
inside it to handle HTTP/S.

## If you like it Buy me a coffe :)

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/paypalme/kendarorg/1)

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
