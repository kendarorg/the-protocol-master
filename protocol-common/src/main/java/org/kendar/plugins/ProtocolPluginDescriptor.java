package org.kendar.plugins;

import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;

import java.lang.reflect.ParameterizedType;

public abstract class ProtocolPluginDescriptor<T, K, W extends PluginSettings> implements PluginDescriptor<W> {
    protected final static JsonMapper mapper = new JsonMapper();
    private boolean active;
    private String instanceId = "default";
    private PluginApiHandler apiHandler;
    private PluginSettings settings;


    public W getSettings() {
        return (W) settings;
    }

    public Class<?> getSettingClass() {
        if (settings != null) return settings.getClass();
        try {
            var startAt = (Class<?>) this.getClass();
            while (startAt != null) {
                var possibleGenericSuperClass = startAt.getGenericSuperclass();
                if (possibleGenericSuperClass instanceof ParameterizedType) {
                    var gss = (ParameterizedType) possibleGenericSuperClass;
                    var atta = gss.getActualTypeArguments();
                    for (var att : atta) {
                        if (att instanceof Class) {
                            if (PluginSettings.class.isAssignableFrom((Class<?>) att)) {
                                return (Class<?>) att;
                            }
                        }
                    }
                }
                startAt = (Class<?>) startAt.getGenericSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Missing plugin settings");
    }

    public String getInstanceId() {
        return instanceId;
    }

    public PluginApiHandler getApiHandler() {
        if (apiHandler == null) {
            apiHandler = buildApiHandler();
        }
        return apiHandler;
    }

    protected PluginApiHandler buildApiHandler() {
        return new DefaultPluginApiHandler<>(this, getId(), getInstanceId());
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
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        this.instanceId = protocol.getProtocolInstanceId();
        if (this.instanceId == null || this.instanceId.isEmpty()) {
            this.instanceId = "default";
        }
        this.settings = (W) pluginSetting;
        if(settings!=null)setActive(pluginSetting.isActive());
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

    protected void handleActivation(boolean active) {

    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (active != this.active) handleActivation(active);
        this.active = active;
    }

    public void refreshStatus() {
        if (active) {
            active = false;
            setActive(true);
        }
    }

    public void terminate(){

    }
}
