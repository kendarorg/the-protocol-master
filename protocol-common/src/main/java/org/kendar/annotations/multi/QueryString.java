package org.kendar.annotations.multi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describe a parameter on query string
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryString {
    /**
     * Name
     *
     * @return
     */
    String key();

    /**
     * Description
     *
     * @return
     */
    String description() default "";

    /**
     * Type of field
     *
     * @return
     */
    String type() default "string";

    /**
     * Example value
     *
     * @return
     */
    String example() default "string";
}
