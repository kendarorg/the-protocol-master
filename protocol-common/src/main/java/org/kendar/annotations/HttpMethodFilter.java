package org.kendar.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface HttpMethodFilter {

    /**
     * Wethear it should be blocking or not
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

    String method() default "";

    String description() default "";

    String id() default "";

    TpmMatcher[] matcher() default {};
}
