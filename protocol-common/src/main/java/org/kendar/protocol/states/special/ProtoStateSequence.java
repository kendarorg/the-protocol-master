package org.kendar.protocol.states.special;

import org.kendar.protocol.states.ProtoState;

public class ProtoStateSequence extends SpecialProtoState {

    public ProtoStateSequence(ProtoState... states) {
        super(states);
    }
}
