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

## Missing features

* [Inline commands](https://redis.io/docs/latest/develop/reference/protocol-spec/#inline-commands) issued via Telnet

## Documentation used

* https://redis.io/docs/reference/protocol-spec/
* https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md
* https://www.memetria.com/blog/beginners-guide-to-redis-protocol
* https://redis.pjam.me/post/chapter-5-redis-protocol-compatibility/ (for inline protocol)

## Weirdness

### Splitted commands

The command identifier, if composed by two strings, is splitted! A command like
"ACL CAT" (see https://redis.io/docs/latest/commands/command-docs/ ) is in fact
splitted, in the array sent by the protocol like this

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

Byte arrays are written and considered as unicode strings, see the specific test
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

The serialization to json has some differences from the standard one, this because
there is no direct matching for standard java collection. Therefore there are
"magic keys" on the collections to identify them. E.G.

```
    "mySet":[
        "@@SET@@",
        "item":[....],
        25,
        12E-18
        ...
```

Maps are defined as arrays of arrays, because in REDIS keys can be any vakye

```
    "myMap":[
        "@@MAP@@",
        ["key","value"]
        ...    
```

* @@SET@@: Defines a RESP set ( https://redis.io/docs/latest/develop/reference/protocol-spec/#sets )
* @@MAP@@: Defines a RESP map ( https://redis.io/docs/latest/develop/reference/protocol-spec/#maps )
* @@PUSH@@: Defines a RESP out of band push ( https://redis.io/docs/latest/develop/reference/protocol-spec/#pushes )

Optionally it is possible to add the following text row on arrays: @@ARRAY@@. By default
every collection is considered an array unless specified differently (hence the optionality
of the array label)