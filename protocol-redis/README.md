# REDIS RESP3 Protocol

You can directly use the proxy as a normal Redis server

## Configuration

* protocol: redis (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. redis://localhost:1884 )
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
* resetConnectionsOnStart: reset connection on start replaying. When starting replay on an already active server  (
  default true)
* blockExternal: Block calls to real service when not matching (default true)

### publish-plugin

* active: If it is active
* Exposes the API to retrieve the current connections and if they are subscribed to something
* Exposes the API to send a message to a currently active connection

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

* [Inline commands](https://redis.io/docs/latest/develop/reference/protocol-spec/#inline-commands) issued via Telnet

## Documentation used

* https://redis.io/docs/reference/protocol-spec/
* https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md
* https://www.memetria.com/blog/beginners-guide-to-redis-protocol
* https://redis.pjam.me/post/chapter-5-redis-protocol-compatibility/ (for inline protocol)

## Weirdness

### Split commands

The command identifier, if composed by two strings, is split! A command like
"ACL CAT" (see https://redis.io/docs/latest/commands/command-docs/ ) is in fact
split, in the array sent by the protocol like this

```
    *3
    +ACL\r\n\
    +CAT\r\n\
    +argument\r\n\
```

Instead of what was expected

```
    *3
    +ACL CAT\r\n\
    +argument\r\n\
```

### The long integer

The "integer" ( https://redis.io/docs/latest/develop/reference/protocol-spec/#integers ) is any "mathematically" integer
value fitting in a long

### Bits and bytes

Byte arrays are written and considered as UNICODE strings, see the specific test
to check how they are handled:

```
   {
      "constant" : false,
      "connectionId" : 1,
      "index" : 3,
      "input" : {
        "type" : "SET",
        "data" : [ "SET", "foo", "\u0001\u0002\u0003\u0004\u0005\u0006\u0007\r\n" ]
      },
      "output" : {
        "type" : "String",
        "data" : "OK"
      },
      "durationMs" : 1,
      "type" : "SET",
      "caller" : "RESP3"
    }
```

### Notes on serialization

The string seems always serialized as "complex string" prefixed with
`$count\r\ncontent` instead of using `+content`

The serialization to json has some differences from the standard one, this because
there is no direct matching for standard java collection. Therefore, there are
"magic keys" on the collections to identify them. E.G.

```
    "mySet":[
        "@@SET@@",
        "item":[....],
        25,
        12E-18
        ...
```

Maps are defined as arrays of arrays, because in REDIS keys can be any value

```
    "myMap":[
        "@@MAP@@",
        ["key","value"]
        ...    
```

* @@SET@@: Defines a RESP set ( https://redis.io/docs/latest/develop/reference/protocol-spec/#sets )
* @@MAP@@: Defines a RESP map ( https://redis.io/docs/latest/develop/reference/protocol-spec/#maps )
* @@PUSH@@: Defines a RESP out of band push ( https://redis.io/docs/latest/develop/reference/protocol-spec/#pushes )

Optionally it is possible to add the following text row on arrays: @@ARRAY@@. By default,
every collection is considered an array unless specified differently (hence the optionality
of the array label)