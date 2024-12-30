package org.kendar.annotations.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assign a base DNS/address to respond to
 * Mandatory for @FilterClass
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TpmService {
    String name() default "";
    String[] tags() default {};
    TpmScope scope() default TpmScope.GLOBAL;
}
