package org.kendar.filters;

public abstract class ProtocolPluginDescriptor<T, K> implements PluginDescriptor {


    /**
     * @param phase
     * @param request
     * @param response
     * @return true when is blocking
     */
    public abstract boolean handle(ProtocolPhase phase, T in, K out);

    public PluginDescriptor clone() {
        try {
            return this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Should implement clone for " + this.getClass(), e);
        }
    }
}
