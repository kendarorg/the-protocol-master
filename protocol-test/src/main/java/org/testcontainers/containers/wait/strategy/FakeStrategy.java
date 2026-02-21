package org.testcontainers.containers.wait.strategy;

public class FakeStrategy extends AbstractWaitStrategy {
    @Override
    protected void waitUntilReady() {
        System.out.printf("Fake Liveness check ports of %s.%n",
                waitStrategyTarget.getContainerInfo().getName());
    }
}
