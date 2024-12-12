package org.kendar.annotations.multi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Header for the requests
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Header {
    /**
     * Description
     * @return
     */
    String description() default "";

    /**
     * Header key
     * @return
     */
    String key();

    /**
     * Header value
     * @return
     */
    String value() default "";

    /**
     * Header content type
     * @return
     */
    Class<?> type() default String.class;
}
