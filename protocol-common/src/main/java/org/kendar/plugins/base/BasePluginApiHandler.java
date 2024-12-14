package org.kendar.plugins.base;

import org.kendar.apis.FilteringClass;

/**
 * Apis exposed by a global plugin
 */
public interface BasePluginApiHandler extends FilteringClass {

    /**
     * Id of the plugin
     *
     * @return
     */
    String getId();
}
