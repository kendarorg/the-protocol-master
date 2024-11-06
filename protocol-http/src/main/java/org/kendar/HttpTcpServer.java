package org.kendar;

import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.server.TcpServer;

import java.util.function.Supplier;

public class HttpTcpServer extends TcpServer {
    private Runnable runner;
    private Supplier<Boolean> runnig;
    private Runnable stop;

    public HttpTcpServer(NetworkProtoDescriptor protoDescriptor) {
        super(null);
    }

    public boolean isRunning() {
        return runnig.get();
    }
    public void stop() {
        stop.run();
    }

    public void start() {
        this.runner.run();
    }

    public void setRunner(Runnable runner) {
        this.runner = runner;
    }

    public void setIsRunning(Supplier<Boolean> runnig) {

        this.runnig = runnig;
    }

    public void setStop(Runnable stop) {
        this.stop = stop;
    }
}
