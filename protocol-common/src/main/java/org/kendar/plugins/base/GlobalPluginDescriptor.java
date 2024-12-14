package org.kendar.plugins.base;

import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.pf4j.ExtensionPoint;

/**
 * Global plugin descriptor disconnected from protocols
 */
public interface GlobalPluginDescriptor<W extends PluginSettings> extends ExtensionPoint, BasePluginDescriptor<W> {


    /**
     * Initialize with settings
     *
     * @param global
     * @param pluginSettings
     * @return
     */
    GlobalPluginDescriptor initialize(GlobalSettings global, PluginSettings pluginSettings);


    /**
     * Retrieve the eventual APIs
     *
     * @return
     */
    BasePluginApiHandler getApiHandler();
}
