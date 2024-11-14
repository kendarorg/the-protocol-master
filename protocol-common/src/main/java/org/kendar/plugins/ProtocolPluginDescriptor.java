package org.kendar.plugins;

import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;

public abstract class ProtocolPluginDescriptor<T, K> implements PluginDescriptor {

    private boolean active;
    private String instanceId = "default";

    public String getInstanceId() {
        return instanceId;
    }

    public ProtocolPluginDescriptor<T, K> asActive() {
        active = true;
        return this;
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
        handleActivation(active);
        this.active = active;
    }
}
