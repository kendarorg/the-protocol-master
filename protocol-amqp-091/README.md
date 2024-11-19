# AMQP 0.9.1 Protocol

## Plugins

### record-plugin

The data will be stored in the global dataDir

* active: If it is active
* recordSites: list of java regexp for matching hosts to record. When empty everything is recorded

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

* active: If it is active
* matchSites: list of java regexp for matching hosts to replay. When empty everything is replayed
* respectCallDuration: respect the duration of the round trip
* blockExternal: default to true, block any calls to not recorded website

## Documentation used

* https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf (page 31)
* https://docs.vmware.com/en/VMware-RabbitMQ-for-Kubernetes/1/rmq/amqp-wireshark.html
* https://github.com/cloudamqp/amqproxy
* https://crystal-lang.org/api/1.11.2
* https://github.com/wireshark/wireshark/blob/master/epan/dissectors/packet-amqp.c
* https://github.com/bloomberg/amqpprox/tree/main/libamqpprox
* https://stackoverflow.com/questions/18403623/rabbitmq-amqp-basicproperties-builder-values


* v0.9.1 vs 0.10.0
    * https://github.com/rabbitmq/rabbitmq-server/blob/main/deps/rabbitmq_amqp1_0/README.md

## Weirdness

* amqp0-9-1.pdf, section 4.2.3, The size is an INTEGER not a long
* Actually transactions are not supported
