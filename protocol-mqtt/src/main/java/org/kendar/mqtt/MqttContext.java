package org.kendar.mqtt;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

public class MqttContext extends NetworkProtoContext {
    private int protocolVersion;

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
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));

    }
}
