package org.kendar.annotations.multi;

/**
 * Security tags
 */
public @interface TpmSecurity {
    /**
     * Scopes
     * @return
     */
    String[] scopes() default {};

    /**
     * Name
     * @return
     */
    String name();
}
