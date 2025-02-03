package org.kendar.plugins.base;

import org.kendar.apis.FilteringClass;

/**
 * Protocol specific APIs handler
 */
public interface ProtocolApiHandler extends FilteringClass {

    /**
     * Retrieve the instance id of the protocol to which the API handler is connected
     *
     * @return
     */
    String getProtocolInstanceId();

    String getProtocol();
}
