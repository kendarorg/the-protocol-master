package org.kendar.filters;

public enum ProtocolPhase {
    /**
     * Never executed
     */
    NONE("NONE"),
    PRE_CALL("PRE_CALL"),
    CONNECT("CONNECT"),
    POST_CALL("POST_CALL");
    private final String text;

    /**
     * Filter phase
     *
     * @param text for phase
     */
    ProtocolPhase(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
