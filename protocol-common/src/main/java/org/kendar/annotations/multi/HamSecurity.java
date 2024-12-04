package org.kendar.annotations.multi;

public @interface HamSecurity {
    String[] scopes() default {};

    String name();
}
