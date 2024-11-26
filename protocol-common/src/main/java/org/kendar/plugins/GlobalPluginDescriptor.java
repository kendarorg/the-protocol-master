package org.kendar.plugins;

import org.kendar.settings.GlobalSettings;
import org.pf4j.ExtensionPoint;

/**
 * Global plugin descriptor disconnected from protocols
 */
public interface GlobalPluginDescriptor  extends ExtensionPoint {
    /**
     * Id of the plugin
     * @return
     */
    String getId();

    /**
     * Initialize with settings
     * @param global
     * @return
     */
    PluginDescriptor initialize(GlobalSettings global);

    /**
     * Retrieve the settings class
     * @return
     */
    Class<?> getSettingClass();

    /**
     * Terminate the plugin
     */
    void terminate();

    /**
     * Check if active
     * @return
     */
    boolean isActive();

    /**
     * Set active
     * @param active
     */
    void setActive(boolean active);

    /**
     * Force active
     */
    void forceActivation();

    /**
     * Retrieve the eventual APIs
     * @return
     */
    GlobalPluginApiHandler getApiHandler();
}
