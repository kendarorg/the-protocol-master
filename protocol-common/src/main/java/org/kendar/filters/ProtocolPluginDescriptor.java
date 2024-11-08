package org.kendar.filters;

import org.kendar.proxy.FilterContext;
import org.kendar.settings.PluginSettings;

public abstract class ProtocolPluginDescriptor<T, K> implements PluginDescriptor {


    private boolean active;

    /**
     * @param request
     * @param response
     * @param filterContext
     * @param phase
     * @return true when is blocking
     */
    public abstract boolean handle(FilterContext filterContext, ProtocolPhase phase, T in, K out);

    public PluginDescriptor clone() {
        try {
            return this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Should implement clone for " + this.getClass(), e);
        }
    }

    public void setSettings(PluginSettings plugin){
        setActive(plugin.isActive());
    }

    public void setActive(boolean active){
        this.active = active;
    }
    public boolean isActive(){
        return active;
    }
}
