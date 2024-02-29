package org.kendar.protocol.states;

import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

/**
 * Special stop state (internal)
 */
public class Stop implements ProtoStep {

    @Override
    public ReturnMessage run() {
        return null;
    }
}
