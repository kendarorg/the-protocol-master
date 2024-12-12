package org.kendar.annotations.multi;


import org.kendar.apis.utils.ConstantsMime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describe a possible response
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TpmResponse {
    /**
     * Possible examples
     * @return
     */
    Example[] examples() default {};

    /**
     * Mime type
     * @return
     */
    String content() default ConstantsMime.JSON;

    /**
     * Return code
     * @return
     */
    int code() default 200;

    /**
     * Possible returned body
     * @return
     */
    Class<?> body() default Object.class;

    /**
     * Returned headers
     * @return
     */
    Header[] headers() default {};

    /**
     * Description
     * @return
     */
    String description() default "";
}
