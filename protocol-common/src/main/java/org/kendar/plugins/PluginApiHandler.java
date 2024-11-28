package org.kendar.plugins;

/**
 * Protocol specific APIs handler
 */
public interface PluginApiHandler extends GlobalPluginApiHandler {

    /**
     * Retrieve the instance id of the protocol to which the API handler is connected
     *
     * @return
     */
    String getProtocolInstanceId();
}
