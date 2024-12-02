package org.kendar.plugins.base;

import org.kendar.settings.GlobalSettings;
import org.pf4j.ExtensionPoint;

/**
 * Global plugin descriptor disconnected from protocols
 */
public interface GlobalPluginDescriptor extends ExtensionPoint,BasePluginDescriptor {


    /**
     * Initialize with settings
     *
     * @param global
     * @return
     */
    ProtocolPluginDescriptor initialize(GlobalSettings global);



    /**
     * Retrieve the eventual APIs
     *
     * @return
     */
    BasePluginApiHandler getApiHandler();
}
