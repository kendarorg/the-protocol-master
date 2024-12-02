package org.kendar.plugins.base;

import com.sun.net.httpserver.HttpExchange;

/**
 * Apis exposed by a global plugin
 */
public interface BasePluginApiHandler {
    /**
     * Handle http request
     *
     * @param apiServerHandler
     * @param exchange
     * @param pathPart
     * @return
     */
    boolean handle(BaseApiServerHandler apiServerHandler, HttpExchange exchange, String pathPart);

    /**
     * Id of the plugin
     *
     * @return
     */
    String getId();
}
