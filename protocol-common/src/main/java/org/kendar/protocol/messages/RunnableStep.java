package org.kendar.protocol.messages;

import java.util.function.Supplier;

public class RunnableStep implements ProtoStep {
    private final Supplier<ReturnMessage> supplier;

    public RunnableStep(Supplier<ReturnMessage> supplier) {

        this.supplier = supplier;
    }

    public RunnableStep(Runnable runnable) {
        this.supplier = () -> {
            runnable.run();
            return null;
        };
    }

    @Override
    public ReturnMessage run() {
        return supplier.get();
    }
}
