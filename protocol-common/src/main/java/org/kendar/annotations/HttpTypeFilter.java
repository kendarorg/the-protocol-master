package org.kendar.annotations;

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
public @interface HttpTypeFilter {
    /**
     * Can be set using simple ${property:default} or ${property}
     * Usually it is * (or anything)
     * @return host
     */
    String hostAddress() default "*";

    /**
     * Can be set using simple ${property:default} or ${property}
     *
     * @return host regexp
     */
    String hostPattern() default "";

    /**
     * The name of
     * @return
     */
    String name() default "";

    /**
     * The priority of the filter (not used)
     * @return
     */
    int priority() default 100;

    /**
     * If blocking the execution is ALWAYS blocked regardless of
     * the methods returning false
     * @return if is blocking
     */
    boolean blocking() default false;
}
