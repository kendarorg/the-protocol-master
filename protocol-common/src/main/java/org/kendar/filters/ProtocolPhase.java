package org.kendar.filters;

public enum ProtocolPhase {
    /**
     * Never executed
     */
    NONE("NONE"),
    PRE_CALL("PRE_CALL"),
    CONNECT("CONNECT"),
    POST_CALL("POST_CALL"),
    PRE_RENDER("PRE_RENDER"),
    ASYNC_RESPONSE("ASYNC_RESPONSE"),
    /**
     * Always blocking
     */
    API("API"),
    STATIC("STATIC"),
    POST_RENDER("POST_RENDER");
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
