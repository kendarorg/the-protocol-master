package org.kendar.plugins.base;

/**
 * Protocol specific APIs handler
 */
public interface ProtocolPluginApiHandler extends BasePluginApiHandler {

    /**
     * Retrieve the instance id of the protocol to which the API handler is connected
     *
     * @return
     */
    String getProtocolInstanceId();
}
