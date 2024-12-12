package org.kendar.annotations;

import java.lang.annotation.*;

/**
 * Matcher function for method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface TpmMatcher {
    String value();

    MatcherFunction function() default MatcherFunction.STARTS;

    MatcherType type();

    String id() default "";
}
