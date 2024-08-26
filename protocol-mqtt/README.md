# MQTT Protocol

You can directly use the proxy as a normal MQTT server (with no authentication
at the moment)

## Missing features

Authentication (AUTH packet)

## Documentation used

* https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html
* https://developer.ibm.com/articles/iot-mqtt-why-good-for-iot/
* https://cedalo.com/blog/mqtt-packet-guide/ Very clear wire-protocol level
* https://vasters.com/archive/2017-01-09-From-MQTT-to-AMQP-and-back.html
* https://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html

## Interesting informations

At the moment the test is based on the moquette MQTT server, since it can be 
embedded directly into any java application and does not require testcontainers

The client used is the paho mqtt client for Java, it's the simplest I have
found. There is only a small trick to force the creation of a PINGREQ packet
that involve a couple of reflection tricks (check the MqttClientHack class in the
test section)

When the Paho client does not know what packet identifier it should generate, 
simply create a new one. This happened for example when during development i 
had not set the packet Identifier when returning a PUBREL packet
