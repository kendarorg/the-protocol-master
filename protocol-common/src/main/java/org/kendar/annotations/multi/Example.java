package org.kendar.annotations.multi;

public @interface Example {
    String exampleFunction() default "";

    String example() default "";

    String description() default "";
}
