package org.kendar.plugins.base;

import org.kendar.utils.JsonMapper;

public class ProtocolPluginApiHandlerDefault<T extends ProtocolPluginDescriptor> implements ProtocolPluginApiHandler {
    protected static final JsonMapper mapper = new JsonMapper();
    private final T descriptor;
    private final String id;
    private final String instanceId;

    public ProtocolPluginApiHandlerDefault(T descriptor, String id, String instanceId) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
    }

    public T getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return id;
    }

    public String getProtocolInstanceId() {
        return instanceId;
    }

}
