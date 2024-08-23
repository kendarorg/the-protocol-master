## MQTT Protocol

## Documentation used

* https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html
* https://developer.ibm.com/articles/iot-mqtt-why-good-for-iot/
* https://cedalo.com/blog/mqtt-packet-guide/ Very clear wire-protocol level
* https://vasters.com/archive/2017-01-09-From-MQTT-to-AMQP-and-back.html
* https://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html

<pre>
    *** To server
    CLIENT   SERVER 
    publish->
           <-PUBREC
    PUBREL->
           <-pubcomp
    *** From server
    CLIENT 	SERVER
        <-PUBLISH
    PUBREC->
        <-PUBREL
    PUBCOMP
</pre>