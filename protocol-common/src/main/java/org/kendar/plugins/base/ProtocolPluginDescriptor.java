package org.kendar.plugins.base;

import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.ExtensionPoint;

import java.util.List;

/**
 * Basic Protocol Plugin
 */
public interface ProtocolPluginDescriptor<W extends PluginSettings> extends
        ExtensionPoint, BasePluginDescriptor<W> {
    /**
     * Phases for the protocol
     *
     * @return
     */
    List<ProtocolPhase> getPhases();

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
    ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting);

    /**
     * Return all the APIs connected with the plugin
     *
     * @return
     */
    List<ProtocolPluginApiHandler> getApiHandler();

    /**
     * Retrieve the specific instance of the protocol connected
     * with the plugin
     *
     * @return
     */
    ProtoDescriptor getProtocolInstance();

    void setProtocolInstance(ProtoDescriptor protocol);

    W getSettings();
}
