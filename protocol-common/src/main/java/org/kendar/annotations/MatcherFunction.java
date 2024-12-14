package org.kendar.annotations;

/**
 * The matcher function to use to match the address
 */
public enum MatcherFunction {
    EXACT("EXACT"),
    REGEXP("REGEXP"),
    STARTS("STARTS"),
    CONTAINS("CONTAINS"),
    END("END");

    private final String text;

    MatcherFunction(final String text) {
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
