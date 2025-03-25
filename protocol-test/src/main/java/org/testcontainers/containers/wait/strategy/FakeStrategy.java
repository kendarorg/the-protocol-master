package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

public class FakeStrategy extends AbstractWaitStrategy {
    @Override
    protected void waitUntilReady() {
        System.out.println(String.format("Fake Liveness check ports of %s.",
                waitStrategyTarget.getContainerInfo().getName()));
    }
}
