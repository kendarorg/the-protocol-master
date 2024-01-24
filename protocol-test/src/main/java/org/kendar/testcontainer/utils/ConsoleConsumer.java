package org.kendar.testcontainer.utils;

import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

public class ConsoleConsumer implements Consumer<OutputFrame> {
    @Override
    public void accept(OutputFrame outputFrame) {
        System.out.println(outputFrame.getUtf8String().trim());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Consumer<OutputFrame> andThen(Consumer<? super OutputFrame> after) {
        return Consumer.super.andThen(after);
    }
}
