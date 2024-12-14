package org.kendar.plugins.base;

import org.kendar.settings.PluginSettings;

public interface BasePluginDescriptor<W extends PluginSettings> {
    /**
     * Id of the plugin
     *
     * @return
     */
    String getId();

    /**
     * Retrieve the settings class
     *
     * @return
     */
    Class<?> getSettingClass();

    /**
     * Terminate the plugin
     */
    void terminate();

    /**
     * Check if active
     *
     * @return
     */
    boolean isActive();

    /**
     * Set active
     *
     * @param active
     */
    void setActive(boolean active);

    /**
     * Clone, this is need to overcome the missing construtors in plugin system
     *
     * @return
     */
    BasePluginDescriptor duplicate();


    void refreshStatus();
}
