package org.kendar.protocol.fsm;

public class ProtoLine {
    private final ProtoState state;
    private final ProtoState[] possibleStates;

    public ProtoLine(ProtoState state, ProtoState[] possibleStates) {

        this.state = state;
        this.possibleStates = possibleStates;
    }

    public ProtoState getState() {
        return state;
    }

    public ProtoState[] getPossibleStates() {
        return possibleStates;
    }
}
