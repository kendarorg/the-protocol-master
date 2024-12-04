package org.kendar.annotations;

import org.kendar.annotations.multi.*;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface TpmDoc {
    String[] tags() default {};

    boolean todo() default false;

    String description() default "";

    String produce() default "";

    QueryString[] query() default {};

    PathParameter[] path() default {};

    Header[] header() default {};

    TpmRequest[] requests() default {};

    TpmResponse[] responses() default {};

    TpmSecurity[] security() default {};
}
