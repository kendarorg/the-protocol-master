package org.kendar.protocol.states.special;

import org.kendar.protocol.states.ProtoState;

import java.util.List;

public class ProtoStateRef extends SpecialProtoState {
    private final String ref;

    public ProtoStateRef(String ref) {

        this.ref = ref;
    }

    @Override
    public List<ProtoState> getChildren() {
        return null;
    }

    public String getRef() {
        return ref;
    }
}
