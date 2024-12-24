package org.kendar.mqtt;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.mqtt.utils.MqttProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.HashSet;
import java.util.stream.Collectors;

public class MqttContext extends NetworkProtoContext {
    private int protocolVersion;
    private HashSet<Integer> usedPackets = new HashSet<>();

    public MqttContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.setValue("PROTOCOL_VERSION", protocolVersion);
    }

    public boolean isVersion(int expectedVersion) {
        return protocolVersion == expectedVersion;
    }

    @Override
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new MqttBBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    @Override
    public void disconnect(Object connection) {
        super.disconnect(connection);
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (MqttProxySocket) conn.getConnection();
        if (sock != null) {
            sock.close();
        }

    }

    public void usePacket(int packetId) {
        usedPackets.add(packetId);
    }

    public int packetToUse() {
        var list = usedPackets.stream().sorted(Integer::compare).collect(Collectors.toList());
        var maxFoundedIndex = 28000;
        for(var i = list.size() - 1; i >= 0; i--){
            var index = list.get(i);
            if(index >14000){
                maxFoundedIndex = index;
            }else{
                break;
            }
        }
        if(maxFoundedIndex==28000){
            usedPackets.clear();
        }
        usedPackets.add(maxFoundedIndex-1);
        return maxFoundedIndex-1;
    }
}
