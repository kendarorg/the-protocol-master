# REDIS RESP3 Protocol

## Notes on serialization

The connection string to use on the runner will be "redis://host:port"

The serialization to json has some differences from the standard one, this because
there is no direct matching for standard java collection. Therefore there are
"magic keys" on the collections to identify them. E.G.

<pre>
    "mySet":[
        "@@SET@@",
        "item":[....],
        25,
        12E-18
        ...
</pre>

Maps are defined as arrays of arrays, because in REDIS keys can be any vakye

<pre>
    "myMap":[
        "@@MAP@@",
        ["key","value"]
        ...    
</pre>

* @@SET@@: Defines a RESP set ( https://redis.io/docs/latest/develop/reference/protocol-spec/#sets )
* @@MAP@@: Defines a RESP map ( https://redis.io/docs/latest/develop/reference/protocol-spec/#maps )
* @@PUSH@@: Defines a RESP out of band push ( https://redis.io/docs/latest/develop/reference/protocol-spec/#pushes )

Optionally it is possible to add the following text row on arrays: @@ARRAY@@. By default
every collection is considered an array unless specified differently (hence the optionality
of the array label)

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

<pre>
*3
+ACL\r\n\
+CAT\r\n\
+argument\r\n\
</pre>

Instead of what was expected

<pre>
*3
+ACL CAT\r\n\
+argument\r\n\
</pre>

### The long integer

The "integer" ( https://redis.io/docs/latest/develop/reference/protocol-spec/#integers ) is any "mathematically" integer
value fitting in a long

### Bits and bytes

Byte arrays are written and considered as unicode strings, see the specific test
to check how they are handled:

<pre>
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
</pre>