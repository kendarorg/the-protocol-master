# MonoDb Protocol

You can directly use the "proxy" as a normal mongodb backend

## Login

If setting login and password on connection string should set "authMechanism=PLAIN". This
is the only supported auth mechanism for the proxy

The connection string to connect to the proxy must always be without login and passwords,
the SaslStart message is not supported

## Missing features

* Real authentication (always allowed)

## Documentation used

* https://www.mongodb.com/docs/manual/reference/mongodb-wire-protocol/
* https://github.com/mongodb/specifications/tree/master
* https://github.com/bwaldvogel/mongo-java-server

## Interesting information

### Document format

The document starts with the Int32 length

<pre>
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

</pre>