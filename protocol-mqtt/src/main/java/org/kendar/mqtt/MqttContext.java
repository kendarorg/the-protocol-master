package org.kendar.mqtt;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

public class MqttContext extends NetworkProtoContext {
    public MqttContext(ProtoDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
//        var sock = (MqttProxySocket) conn.getConnection();
//        if (sock != null) {
//            sock.close();
//        }
    }
}
