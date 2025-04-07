package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PortWaitStrategy extends AbstractWaitStrategy {

    private int[] ports;

    @Override
    protected void waitUntilReady() {
        System.out.println(String.format("Liveness check ports of %s starting.",
                waitStrategyTarget.getContainerInfo().getName()));
        final Set<Integer> externalLivenessCheckPorts;
        if (this.ports == null || this.ports.length == 0) {
            externalLivenessCheckPorts = getLivenessCheckPorts();
            if (externalLivenessCheckPorts.isEmpty()) {
                System.out.println(String.format("Liveness check ports of %s is empty. Not waiting.",
                        waitStrategyTarget.getContainerInfo().getName()));
                return;
            }
        } else {
            externalLivenessCheckPorts =
                    Arrays
                            .stream(this.ports)
                            .mapToObj(port -> waitStrategyTarget.getMappedPort(port))
                            .collect(Collectors.toSet());
        }

        List<Integer> exposedPorts = waitStrategyTarget.getExposedPorts();

        final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> internalCheck = new InternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(
                waitStrategyTarget,
                externalLivenessCheckPorts
        );
        AtomicBoolean blocking = new AtomicBoolean(false);
        AtomicBoolean polling = new AtomicBoolean(false);

        try {
            List<Future<Boolean>> futures = EXECUTOR.invokeAll(
                    Arrays.asList(
                            // Blocking
                            () -> {
                                Instant now = Instant.now();
                                Boolean result = internalCheck.call();
                                System.out.println(String.format("Internal port check %s for %s in %dms",
                                        Boolean.TRUE.equals(result) ? "passed" : "failed",
                                        String.join(",", internalPorts.stream().map(Object::toString).toList()),
                                        Duration.between(now, Instant.now()).toMillis()));
                                blocking.set(result);
                                return result;
                            },
                            // Polling
                            () -> {
                                System.out.println(String.format(
                                        "External port check started for %s mapped as %s",
                                        String.join(",", internalPorts.stream().map(Object::toString).toList()),
                                        String.join(",", externalLivenessCheckPorts.stream().map(Object::toString).toList())
                                ));
                                Instant now = Instant.now();
                                Awaitility
                                        .await()
                                        .pollInSameThread()
                                        .pollInterval(Duration.ofMillis(100))
                                        .pollDelay(Duration.ZERO)
                                        .failFast("container is no longer running", () -> !waitStrategyTarget.isRunning())
                                        .ignoreExceptions()
                                        .forever()
                                        .until(externalCheck);

                                System.out.println(String.format(
                                        "External port check passed for %s mapped as %s in %dms",
                                        String.join(",", internalPorts.stream().map(Object::toString).toList()),
                                        String.join(",", externalLivenessCheckPorts.stream().map(Object::toString).toList()),
                                        Duration.between(now, Instant.now()).toMillis()
                                ));
                                polling.set(true);
                                return true;
                            }
                    ),
                    startupTimeout.getSeconds(),
                    TimeUnit.SECONDS
            );

            for (Future<Boolean> future : futures) {
                future.get(0, TimeUnit.SECONDS);
            }
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException e) {
            System.err.println("Timed out waiting for container port to open (" +
                    waitStrategyTarget.getHost() +
                    " ports: " +
                    externalLivenessCheckPorts +
                    " should be listening)");
            throw new ContainerLaunchException(
                    "Timed out waiting for container port to open (" +
                            waitStrategyTarget.getHost() +
                            " ports: " +
                            externalLivenessCheckPorts +
                            " should be listening)"
            );
        }
    }

    private Set<Integer> getInternalPorts(Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
        return exposedPorts
                .stream()
                .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
                .collect(Collectors.toSet());
    }

    public PortWaitStrategy forPorts(int... ports) {
        this.ports = ports;
        return this;
    }
}