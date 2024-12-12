package org.kendar.annotations;

import java.lang.annotation.*;

/**
 * Annotate a method of a @FilterClass to use it as a controller method
 * Mandatory for Controller methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface HttpMethodFilter {

    /**
     * Weather it should be blocking or not
     *
     * @return if is blocking
     */
    boolean blocking() default false;

    /**
     * Can be set using simple ${property:default} or ${property}
     *
     * @return the path
     */
    String pathAddress() default "";

    /**
     * Can be set using simple ${property:default} or ${property}
     *
     * @return property pattern
     */
    String pathPattern() default "";

    /**
     * The HTTP method (GET/PUT/POST/DELETE/PATCH/OPTIONS)
     * @return
     */
    String method() default "";

    /**
     * The description on swagger
     * @return
     */
    String description() default "";

    /**
     * The unique (to application) id of the method
     * @return
     */
    String id() default "";

    /**
     * The matcher (not in use)
     * @return
     */
    TpmMatcher[] matcher() default {};
}
