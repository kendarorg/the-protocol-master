package org.kendar.annotations;

import org.kendar.annotations.multi.*;

import java.lang.annotation.*;

/**
 * Define the documentation for the given method (Optional)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface TpmDoc {
    /**
     * Tags on swagger rendering
     *
     * @return
     */
    String[] tags() default {};

    boolean todo() default false;

    String description() default "";

    /**
     * Http return type
     *
     * @return
     */
    String produce() default "";

    /**
     * Query string parameters
     *
     * @return
     */
    QueryString[] query() default {};

    /**
     * Path parameters
     *
     * @return
     */
    PathParameter[] path() default {};

    /**
     * Headers associated with method
     *
     * @return
     */
    Header[] header() default {};

    /**
     * Kinds of requests
     *
     * @return
     */
    TpmRequest[] requests() default {};

    /**
     * Kinds of responses
     *
     * @return
     */
    TpmResponse[] responses() default {};

    /**
     * Security descriptors
     *
     * @return
     */
    TpmSecurity[] security() default {};
}
