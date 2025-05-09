# MQTT Protocol

You can directly use the proxy as a normal MQTT server (with no authentication
at the moment)

## Configuration

* protocol: mqtt (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. tcp://localhost:1884 )
* timeoutSeconds: the timeout to drop the connections
* ignoreTrivialCalls: all calls must be present
* blockExternal: if a call is not matched then try to use the real connection

Uses the following phases

* PRE_CALL (Before calling the real server)
* POST_CALL
* PRE_SOCKET_WRITE (Before sending data to the client)
* ASYNC_RESPONSE (When receiving push data from the server)

## Plugins

### record-plugin

The data will be stored in the global dataDir.

All callback are recorded

* active: If it is active
* ignoreTrivialCalls: store in full only calls that cannot be generated automatically (the ones with real data)

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

All callback are replayed automatically

* active: If it is active
* respectCallDuration: respect the duration of the round trip
* resetConnectionsOnStart: reset connection on start replaying. When starting replay on an already active server  (
  default true)
* blockExternal: Block calls to real service when not matching (default true)
* resetConnectionsOnStart: reset connection on start replaying. When starting replay on an already active server (
  default true)

### publish-plugin

* active: If it is active
* Exposes the API to retrieve the current connections and if they are subscribed to something
* Exposes the API to send a message to a currently active connection

The qos is retrieved from the subscribed topic

### report-plugin

Send all activity on the internal events queue (the default subscriber if active is the global-report-plugin)

* active: If it is active

### network-error-plugin

Change random bytes on the data sent back to the client

* active: If it is active
* percentAction: the percent of calls to generate errors

### latency-plugin

Introduce random latency, not applicable to async calls

* active: If it is active
* minMs: Minimum latency added (default 0)
* maxMs: Max latency added (default 0)

### rest-plugins-plugin

This plugin is used to intercept protocol calls and forward the request to a REST API
that will need to respond with the correct response data. Contains a list of "interceptors"
definitions. For details on the implementation [here](../docs/rest-plugins-plugin.md)

* name: The name of the interceptor
* destinationAddress: The api to call (POST)
* inputType: The expected input type (simple class name), Object for any
* inMatcher: The matcher for the in content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string from contains
* outputType: The expected output type (simple class name), Object for any
* outMatcher: The matcher for the out content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string from contains
* blockOnException: If there is an exception return the error and stop the filtering

## Missing features

* Authentication (AUTH packet)
* Session Clean
* SSL

## Documentation used

* https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html
* https://developer.ibm.com/articles/iot-mqtt-why-good-for-iot/
* https://cedalo.com/blog/mqtt-packet-guide/ Very clear wire-protocol level
* https://vasters.com/archive/2017-01-09-From-MQTT-to-AMQP-and-back.html
* https://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html

## Connection String

* The connection string for the proxy is tcp://[host]:[port]
* User id and password currently blank

## Interesting information

```
Remember that with automatic recconection with Eclipse Paho
YOU MUST re-connect and re-subscribe!!! 

EVEN if yout set the reconnect to "true"
```

* [Ref.1](https://github.com/eclipse-paho/paho.mqtt.java/issues/686)
* [Ref.2](https://stackoverflow.com/a/33735501/1632288)

At the moment the test is based on the moquette MQTT server, since it can be
embedded directly into any java application and does not require testcontainers

The client used is the paho mqtt client for Java, it's the simplest I have
found. There is only a small trick to force the creation of a `PINGREQ` packet
that involve a couple of reflection tricks (check the MqttClientHack class in the
test section)

When the Paho client does not know what packet identifier it should generate,
simply create a new one. This happened for example when during development. I
had not set the packet Identifier when returning a `PUBREL` packet

The packets can arrive even in out of order kind...
