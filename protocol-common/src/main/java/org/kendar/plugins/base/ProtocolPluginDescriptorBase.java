package org.kendar.plugins.base;

import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProtocolPluginDescriptorBase<W extends PluginSettings> implements ProtocolPluginDescriptor<W> {
    protected final static JsonMapper mapper = new JsonMapper();
    private boolean active;
    private String instanceId = "default";
    private ProtocolPluginApiHandler apiHandler;
    private PluginSettings settings;
    private ProtoDescriptor protocolInstance;


    public ProtoDescriptor getProtocolInstance() {
        return protocolInstance;
    }

    public void setProtocolInstance(ProtoDescriptor procotolInstance) {
        this.protocolInstance = procotolInstance;
    }

    private static final Logger log = LoggerFactory.getLogger(ProtocolPluginDescriptorBase.class);

    public W getSettings() {
        return (W) settings;
    }

    public Class<?> getSettingClass() {
        if (settings != null) return settings.getClass();
        return PluginSettings.class;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public ProtocolPluginApiHandler getApiHandler() {
        if (apiHandler == null) {
            apiHandler = buildApiHandler();
        }
        return apiHandler;
    }

    protected ProtocolPluginApiHandler buildApiHandler() {
        return new ProtocolPluginApiHandlerDefault<>(this, getId(), getInstanceId());
    }


    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        this.instanceId = protocol.getProtocolInstanceId();
        if (this.instanceId == null || this.instanceId.isEmpty()) {
            this.instanceId = "default";
        }
        this.settings = pluginSetting;
        if (settings != null) setActive(pluginSetting.isActive());
        log.debug("Init plugin {} {} {}",this.getInstanceId(),this.getProtocol(),this.getId());
        return this;
    }

    public ProtocolPluginDescriptor duplicate() {
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
        var isChanged = active!=this.isActive();
        if (isChanged) handleActivation(active);
        this.active = active;
        if(isChanged)handlePostActivation(active);
    }

    protected void handlePostActivation(boolean active) {

    }

    public void refreshStatus() {
        if (active) {
            active = false;
            setActive(true);
        }
    }

    public void terminate() {

    }
}
