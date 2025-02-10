package org.kendar.plugins.base;

import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base descriptor for a protocol plugin
 *
 * @param <W>
 */
public abstract class ProtocolPluginDescriptorBase<W extends PluginSettings> implements ProtocolPluginDescriptor<W> {
    private static final Logger log = LoggerFactory.getLogger(ProtocolPluginDescriptorBase.class);
    protected final JsonMapper mapper;
    private boolean active;
    private String instanceId = "default";
    private List<ProtocolPluginApiHandler> apiHandler;
    private PluginSettings settings;
    private ProtoDescriptor protocolInstance;

    public ProtocolPluginDescriptorBase(JsonMapper mapper) {

        this.mapper = mapper;
    }

    public ProtoDescriptor getProtocolInstance() {
        return protocolInstance;
    }

    public void setProtocolInstance(ProtoDescriptor procotolInstance) {
        if (procotolInstance == null) {
            return;
        }
        this.protocolInstance = procotolInstance;
    }

    /**
     * Retrieve the settings
     *
     * @return
     */
    public W getSettings() {
        return (W) settings;
    }

    /**
     * Set the settings
     * @param settings
     */
    public void setSettings(W settings) {
        this.settings = settings;
        handleSettingsChanged();
    }

    /**
     * Handle changing settings
     */
    protected boolean handleSettingsChanged() {
        return true;
    }

    /**
     * Retrieve the settings class (For deserializaton purposes
     *
     * @return
     */
    public Class<?> getSettingClass() {
        if (settings != null) return settings.getClass();
        return PluginSettings.class;
    }

    /**
     * The protocol instance ID
     *
     * @return
     */
    public String getInstanceId() {
        return instanceId;
    }

    public List<ProtocolPluginApiHandler> getApiHandler() {
        if (apiHandler == null) {
            apiHandler = buildApiHandler();
        }
        return apiHandler;
    }

    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new ProtocolPluginApiHandlerDefault<>(this, getId(), getInstanceId()));
    }


    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        this.instanceId = protocol.getProtocolInstanceId();
        if (this.instanceId == null || this.instanceId.isEmpty()) {
            this.instanceId = "default";
        }
        this.settings = pluginSetting;
        if (settings != null) setActive(pluginSetting.isActive());
        log.debug("Init plugin {} {} {}", this.getInstanceId(), this.getProtocol(), this.getId());
        return this;
    }

    public ProtocolPluginDescriptor duplicate() {
        try {
            return this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Should implement clone for " + this.getClass(), e);
        }
    }

    /**
     * Actiavion callback
     *
     * @param active
     */
    protected void handleActivation(boolean active) {

    }

    public boolean isActive() {
        return active;
    }

    /**
     * Handle the activation of the plugin
     *
     * @param active
     */
    public void setActive(boolean active) {
        var isChanged = active != this.isActive();
        if (isChanged) handleActivation(active);
        this.active = active;
        if (isChanged) handlePostActivation(active);
    }

    /**
     * Post activation callback
     *
     * @param active
     */
    protected void handlePostActivation(boolean active) {

    }

    public void refreshStatus() {
        if (active) {
            active = false;
            setActive(true);
        }
    }

    /**
     * Terminate the plugin
     */
    public void terminate() {

    }


}
