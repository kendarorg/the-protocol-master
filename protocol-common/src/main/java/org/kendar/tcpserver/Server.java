package org.kendar.tcpserver;

import org.kendar.protocol.descriptor.ProtoDescriptor;

import java.io.File;

public interface Server {
    void stop();

    boolean isRunning();

    void setOnStart(Runnable onStart);

    void start();
    ProtoDescriptor getProtoDescriptor();

    void enableTls(File certificateFile, File privateKeyFile);
    void enableSelfSignedTls();
}
