package org.kendar.tcpserver;

import org.kendar.protocol.descriptor.ProtoDescriptor;

public interface Server {
    void stop();

    boolean isRunning();

    void setOnStart(Runnable onStart);

    void start();
    ProtoDescriptor getProtoDescriptor();
}
