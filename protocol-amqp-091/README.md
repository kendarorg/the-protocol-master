## AMQP 0.9.1 Protocol

## Documentation used

* https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf (page 31)
* https://docs.vmware.com/en/VMware-RabbitMQ-for-Kubernetes/1/rmq/amqp-wireshark.html
* https://dzone.com/refcardz/amqp-essentials (amqp 1.0)
* https://github.com/cloudamqp/amqproxy
* https://crystal-lang.org/api/1.11.2
* https://github.com/wireshark/wireshark/blob/master/epan/dissectors/packet-amqp.c
* https://github.com/bloomberg/amqpprox/tree/main/libamqpprox
* https://stackoverflow.com/questions/18403623/rabbitmq-amqp-basicproperties-builder-values
* https://www.ascii-code.com/


* v0.9.1 vs 0.10.0
    * https://github.com/rabbitmq/rabbitmq-server/blob/main/deps/rabbitmq_amqp1_0/README.md

## Weirdness

amqp0-9-1.pdf, section 4.2.3
The size is an INTEGER not a long
