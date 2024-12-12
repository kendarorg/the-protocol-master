package org.kendar.annotations.multi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describe path paramater
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PathParameter {
    /**
     * Name of the parameter (must match the ones in @HttpMethodFilter
     * @return
     */
    String key();

    /**
     * Description
     * @return
     */
    String description() default "";

    /**
     * Type
     * @return
     */
    String type() default "string";

    /**
     * Example value
     * @return
     */
    String[] example() default "string";

    /**
     * List of allowed values
     * @return
     */
    String[] allowedValues() default "";
}
