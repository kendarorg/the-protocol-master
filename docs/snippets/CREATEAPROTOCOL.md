## Add a new protocol to the Protocol Master

Here the journey to add MQTT protocol to The Protocol Master (tpm from now on). It will use the direct
proxy (that means that will directly forward the messages to the real broker)
and will of course accept push messages from the server

### Pom.xml

Add first to the pom.xml the dependencies on the main projects.

* Jaxb for the serialization/deserialization to store the data
* Common for the common stuffs
* Test for the testcontainers and general test utilities

```
    <dependency>
        <groupId>javax.xml.bind</groupId>
        <artifactId>jaxb-api</artifactId>
        <version>${jaxb.api.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-common</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-test</artifactId>
        <version>${revision}</version>
        <scope>test</scope>
    </dependency>
```

### MqttProtocol class

Create the protocol file e.g. "MqttProtocol" extending
"NetworkProtoDescriptor". We are creating a network based communication!

This is the definition of the protocol.

The byte ordering follows the [Big Endian](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html)
and the default port is 1883.

* Constructor with and without ports
* consumeContext, with all the currently running contexts

```
    private static final int PORT = 1883;
    private int port = PORT;
    public static ConcurrentHashMap<Integer, NetworkProtoContext> consumeContext;

    private MqttProtocol() {
        consumeContext = new ConcurrentHashMap<>();
    }

    public MqttProtocol(int port) {this();this.port = port;}
    @Override
    public boolean isBe() {return false;}

    @Override
    public int getPort() {return port;}
```

### MqttContext

Each client (connection) works inside its specific Context. The context contains all
the data relative to a specific connection. Let's create "MqttContext" extending
"NetworkProtoContext".

The only method is the disconnect. It get the default "CONNECTION" object from
the context specific values and close it.

```
    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
//        var sock = (MqttProxySocket) conn.getConnection();
//        if (sock != null) {
//            sock.close();
//        }
    }
```

Is now possible to initialize the "MqttProtocol.createContext" function. Storing
the context in the contexts cache

```
    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        var result = new MqttContext(protoDescriptor);
        consumeContext.put(result.getContextId(), result);
        return result;
    }
```

### What's inside

All the system is based on

* Events
* States
* Behaviours

Events are sent to an internal queue, then they are matched against a list of behaviour/states
then an outcome is produced, be it an event, a ReturnMessage, an operation to forward to the
proxy or an action to perform

### The base MqttPacket

All the packets arriving from the clients are in the form of a BytesEvent. Basically an empowered byte[].
This is directly the data sent from the socket from the client to tpm

For most protocols bytes are packed in a specific way, usually starting with the length. For Mqtt this
is the "main" packet format

* Fixed Header
    * 4 bits Packet Type
    * 4 bits Flags
    * 1-4 bytes as [Variable Byte Integer](snippets/variablebyteinteger.md)
* x bytes Variable Header, this varies for each message Type
* x bytes Payload, this of course varies for each message

#### Fixed header

The first 8 bytes will be
handled [as a flag](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc511988498)
inside a specific "flag" enum: MqttFixedHeader

Looking at the code we add the method to read/write the flag

```
public enum MqttFixedHeader {
  RESERVED(0x00),
  ....
  boolean isFlagSet(int source, int flag) 
  ....
  int setFlag(int source, int flag)
  ....
  int unsetFlag(int source, int flag)
  ....  
```

The first thing would be to translate the bytes to a MqttPacket. For this we can add
a special "interruptState" implementing InterruptProtoState. This means that this
state is in reality a filter that translates the packets

Since we will be using this very same format we add NetworkProxySplitterState that
is used when receiving data from the real server to understand if the packet has
all the required data.

This event or "packet" will extend the "BaseEvent" and, since the format is
common for the return data, the "NetworkReturnMessage" the latter require
the write method to be implemented. This method writes the buffer that will
be sent back to the client

```
    public class MqttPacket extends BaseEvent implements NetworkReturnMessage
```

#### The variable length integer

To ease the development the BBuffer used will be a new MqttBBuffer, containing
methods specific for the protocol

```
public class MqttBBuffer extends BBuffer {
  void writeVarBInteger(long number)
  int writeVarBInteger(long number, int offset) //returns the length
  long readVarBInteger()
  long readVarBInteger(int offset)
```



