package org.kendar.protocol.fsm;

import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.ReturnMessage;

public class Stop implements ProtoStep {

    @Override
    public ReturnMessage run() {
        return null;
    }
}
