# AMQP 0.9.1 Protocol

Create a two-way transparent proxy to a remote amqp 0.9.1 compatible server.

## Configuration

* protocol: amqp091 (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. amqp://localhost:5372 )
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
* resetConnectionsOnStart: reset connection on start replaying. When starting replay on an already active server (
  default true)

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

All callback are replayed automatically

* active: If it is active
* respectCallDuration: respect the duration of the round trip
* resetConnectionsOnStart: reset connection on start replaying. When starting replay on an already active server (
  default true)
* blockExternal: Block calls to real service when not matching (default true)

### publish-plugin

Exposes two APIs

* Retrieve the current connections and if they are subscribed to something
* Send a message to a currently active connection

* active: If it is active

### report-plugin

Send all activity on the internal events queue (the default subscriber if active is the global-report-plugin)

* active: If it is active

### network-error-plugin

Change random bytes on the data sent back to the client

* active: If it is active
* percentAction: the percent of calls to generate errors

### latency-plugin

Introduce random latency. Not applicable to async calls

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
* inMatcher: The matcher for the in content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string from
  contains
* outputType: The expected output type (simple class name), Object for any
* outMatcher: The matcher for the out content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string
  from contains
* blockOnException: If there is an exception return the error and stop the filtering

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
