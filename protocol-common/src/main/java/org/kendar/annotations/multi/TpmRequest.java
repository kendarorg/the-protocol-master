package org.kendar.annotations.multi;


import org.kendar.apis.utils.ConstantsMime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describe a possible request
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TpmRequest {
    /**
     * List of examples
     *
     * @return
     */
    Example[] examples() default {};

    /**
     * Accepted mime type
     *
     * @return
     */
    String accept() default ConstantsMime.JSON;

    /**
     * Body type
     *
     * @return
     */
    Class<?> body() default Object.class;

    /**
     * Function to call for body type
     *
     * @return
     */
    String bodyMethod() default "";

    /**
     * If optional
     *
     * @return
     */
    boolean optional() default false;
}
