package org.kendar.protocol.fsm;

import java.util.Arrays;
import java.util.List;

public class ProtoLine {
    private final ProtoState state;
    private final List<ProtoState> possibleStates;

    public ProtoLine(ProtoState state, ProtoState[] possibleStates) {

        this.state = state;
        this.possibleStates = Arrays.asList(possibleStates);
    }

    public ProtoState getState() {
        return state;
    }

    public List<ProtoState> getPossibleStates() {
        return possibleStates;
    }
}
