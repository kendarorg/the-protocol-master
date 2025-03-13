# MonoDb Protocol

You can directly use the "proxy" as a normal mongodb backend

## Login

If setting login and password on connection string should set "authMechanism=PLAIN". This
is the only supported auth mechanism for the proxy

The connection string to connect to the proxy must always be without login and passwords,
the SaslStart message is not supported

## Configuration

* protocol: redis (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. mongodb://localhost:27018 )
* timeoutSeconds: the timeout to drop the connections

Uses the following phases

* PRE_CALL (Before calling the real server)
* POST_CALL
* PRE_SOCKET_WRITE (Before sending data to the client)

## Plugins

### record-plugin

The data will be stored in the global dataDir.

* active: If it is active
* ignoreTrivialCalls: store in full only calls that cannot be generated automatically (the ones with real data)

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

* active: If it is active
* respectCallDuration: respect the duration of the round trip
* blockExternal: Block calls to real service when not matching (default true)

### report-plugin

Send all activity on the internal events queue (the default subscriber if active is the global-report-plugin)

* active: If it is active

### network-error-plugin

Change random bytes on the data sent back to the client

* active: If it is active
* percentAction: the percent of calls to generate errors

### latency-plugin

Introduce random latency

* active: If it is active
* minMs: Minimum latency added (default 0)
* maxMs: Max latency added (default 0)

## Missing features

* Real authentication (always allowed)

## Documentation used

* https://www.mongodb.com/docs/manual/reference/mongodb-wire-protocol/
* https://github.com/mongodb/specifications/tree/master
* https://github.com/bwaldvogel/mongo-java-server
* https://www.mongodb.com/docs/manual/reference/command/

## Interesting information

### Document format

The document starts with the Int32 length

```

struct Section {
    uint8 payloadType;
    union payload {
        document  document; // payloadType == 0
        struct sequence { // payloadType == 1
            int32      size;
            cstring    identifier;
            document*  documents;
        };
    };
};

struct OP_MSG {
    struct MsgHeader {
        int32  messageLength;
        int32  requestID;
        int32  responseTo;
        int32  opCode = 2013;
    };
    uint32      flagBits;
    Section+    sections;
    [uint32     checksum;]
};
```