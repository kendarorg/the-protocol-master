package org.kendar.plugins;

import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;

public abstract class ProtocolPluginDescriptor<T, K> implements PluginDescriptor {
    protected final static JsonMapper mapper = new JsonMapper();
    private boolean active;
    private String instanceId = "default";

    public String getInstanceId() {
        return instanceId;
    }
    private PluginApiHandler apiHandler;

    public PluginApiHandler getApiHandler() {
        if(apiHandler == null) {
            apiHandler = buildApiHandler();
        }
        return apiHandler;
    }

    protected PluginApiHandler buildApiHandler() {
        return new DefaultPluginApiHandler(this,getId(),getInstanceId());
    }

    /**
     * @param request
     * @param response
     * @param pluginContext
     * @param phase
     * @return true when is blocking
     */
    public abstract boolean handle(PluginContext pluginContext, ProtocolPhase phase, T in, K out);

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        this.instanceId = protocol.getProtocolInstanceId();
        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PluginDescriptor clone() {
        try {
            return this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Should implement clone for " + this.getClass(), e);
        }
    }

    public void setSettings(PluginSettings plugin) {
        setActive(plugin.isActive());
    }

    protected void handleActivation(boolean active) {

    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (active != this.active) handleActivation(active);
        this.active = active;
    }
}
