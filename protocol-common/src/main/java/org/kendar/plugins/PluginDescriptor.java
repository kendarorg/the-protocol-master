package org.kendar.plugins;

import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.ExtensionPoint;

import java.util.List;

/**
 * Basic Protocol Plugin
 */
public interface PluginDescriptor<W extends PluginSettings> extends ExtensionPoint {
    /**
     * Phases for the protocol
     *
     * @return
     */
    List<ProtocolPhase> getPhases();

    /**
     * Id of the plugin
     *
     * @return
     */
    String getId();

    /**
     * Associable protocols
     *
     * @return
     */
    String getProtocol();

    /**
     * Initialize the plugin and protocol
     *
     * @param global
     * @param protocol
     * @param pluginSetting
     * @return
     */
    PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting);

    /**
     * Terminate the plugin
     */
    void terminate();

    /**
     * Clone, this is need to overcome the missing construtors in plugin system
     *
     * @return
     */
    PluginDescriptor clone();

    /**
     * The settings class
     *
     * @return
     */
    Class<?> getSettingClass();


    boolean isActive();

    void setActive(boolean active);

    void refreshStatus();

    PluginApiHandler getApiHandler();
}
