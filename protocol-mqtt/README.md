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

## Interesting informations

At the moment the test is based on the moquette MQTT server, since it can be
embedded directly into any java application and does not require testcontainers

The client used is the paho mqtt client for Java, it's the simplest I have
found. There is only a small trick to force the creation of a PINGREQ packet
that involve a couple of reflection tricks (check the MqttClientHack class in the
test section)

When the Paho client does not know what packet identifier it should generate,
simply create a new one. This happened for example when during development. I
had not set the packet Identifier when returning a PUBREL packet
