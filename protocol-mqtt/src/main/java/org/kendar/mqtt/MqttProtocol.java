package org.kendar.mqtt;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;

import java.util.concurrent.ConcurrentHashMap;

public class MqttProtocol extends NetworkProtoDescriptor {
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

    @Override
    protected void initializeProtocol() {

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        var result = new MqttContext(protoDescriptor);
        consumeContext.put(result.getContextId(), result);
        return result;
    }
}
