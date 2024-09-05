package org.kendar.testcontainer.utils;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "rawtypes"})
public abstract class BaseImage<T extends BaseImage, K extends GenericContainer> implements AutoCloseable {
    protected List<String> aliases = new ArrayList<>();
    protected List<Integer> ports = new ArrayList<>();
    protected int[] waitPort = new int[]{};

    protected K container;
    private Network network;

    public T start() {
        preStart();
        if (!ports.isEmpty()) {
            container.withExposedPorts(ports.toArray(new Integer[]{}));
        }
        if (!aliases.isEmpty()) {
            if (network == null) {
                throw new RuntimeException("Must use network to use aliases");
            }
            container.withNetworkAliases(aliases.toArray(new String[]{}));
        }
        if (this.waitPort.length > 0) {
            container.waitingFor(Wait.forListeningPorts(waitPort));
        }
        if (this.network != null) {
            container.withNetwork(network);
        }

        container.withReuse(false)
                .start();
        postStart();
        return (T) this;
    }

    protected void postStart() {

    }

    protected abstract void preStart();

    public T withAliases(String... aliases) {
        this.aliases = List.of(aliases);
        return (T) this;
    }

    public T withExposedPorts(Integer... ports) {
        this.ports = List.of(ports);
        return (T) this;
    }

    public T waitingForPort(int... waitPort) {
        this.waitPort = waitPort;
        return (T) this;
    }

    public int getMappedPort(int exposedPort) {
        checkStarted();
        if (ports.stream().noneMatch(p -> p == exposedPort))
            throw new RuntimeException("Port " + exposedPort + " not mapped");
        return container.getMappedPort(exposedPort);
    }

    @Override
    public void close() throws Exception {
        if (isStarted()) {
            container.stop();
        }
    }

    protected void checkStarted() {
        if (!isStarted()) throw new RuntimeException("Container not started " + String.join(",", aliases));
    }

    protected boolean isStarted() {
        if (container != null) return container.isRunning();
        return false;
    }

    public T withNetwork(Network network
    ) {
        try {
            var networkName = System.getProperty("dockerNetworkName");
            if(networkName==null){
                this.network=network;
                return (T) this;
            }

            //XXX https://stackoverflow.com/questions/32120909/how-to-pass-parameter-to-maven-test
            this.network = new Network() {
                @Override
                public Statement apply(Statement statement, Description description) {
                    return null;
                }

                @Override
                public String getId() {
                    return networkName;
                }

                @Override
                public void close() {

                }
            };
        }catch (Exception ex){
            this.network = network;
        }
        return (T) this;
    }

    public String getLogs() {
        return container.getLogs();
    }
}
