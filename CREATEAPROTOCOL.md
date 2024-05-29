## Add a new protocol to the Protocol Master

Here the journey to add MQTT protocol to Protocol Master. It will use the direct
proxy (that means that will directly forward the messages to the real broker)
and will of course accept push messages from the server

### Pom.xml

Add first to the pom.xml the dependencies on the main projects.

* Jaxb for the serialization/deserialization to store the data
* Common for the common stuffs
* Test for the testcontainers and general test utilities

<pre>
    <dependency>
        <groupId>javax.xml.bind</groupId>
        <artifactId>jaxb-api</artifactId>
        <version>${jaxb.api.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-test</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
</pre>

### MqttProtocol class

Create the protocol file e.g. "MqttProtocol" extending 
"NetworkProtoDescriptor". We are creating a network based communication!

This is the definition of the protocol.

The byte ordering follows the [Big Endian](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html) 
and the default port is 1883.

* Constructor with and without ports
* consumeContext, with all the currently running contexts

<pre>
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
</pre>

### MqttContext

Each client (connection) works inside its specific Context. The context contains all
the data relative to a specific connection. Let's create "MqttContext" extending
"NetworkProtoContext".

The only method is the disconnect. It get the default "CONNECTION" object from
the context specific values and close it.

<pre>
    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
//        var sock = (MqttProxySocket) conn.getConnection();
//        if (sock != null) {
//            sock.close();
//        }
    }
</pre>

Is now possible to initialize the "MqttProtocol.createContext" function. Storing
the context in the contexts cache

<pre>
    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        var result = new MqttContext(protoDescriptor);
        consumeContext.put(result.getContextId(), result);
        return result;
    }
</pre>

### What's inside

All the system is based on

* Events
* States
* Behaviours

Events are sent to an internal queue, then they are matched against a behaviour in the states
then an outcome is produced, be it an event, a ReturnMessage, an operation to forward to the
proxy or an action to perform

### Packets translation

All the packets arriving from the clients are in the form of a BytesEvent. Basically an empowered byte[].


