package org.kendar.plugins.base;

import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.ExtensionPoint;

import java.util.List;

/**
 * Basic Protocol Plugin
 */
public interface ProtocolPluginDescriptor<T,K,W extends PluginSettings> extends ExtensionPoint, BasePluginDescriptor<W> {
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
     * @param request
     * @param response
     * @param pluginContext
     * @param phase
     * @return true when is blocking
     */
    boolean handle(PluginContext pluginContext, ProtocolPhase phase, T in, K out);

    ProtocolPluginApiHandler getApiHandler();

}
