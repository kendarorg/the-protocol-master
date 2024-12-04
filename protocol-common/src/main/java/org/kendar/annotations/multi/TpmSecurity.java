package org.kendar.annotations.multi;

public @interface TpmSecurity {
    String[] scopes() default {};

    String name();
}
