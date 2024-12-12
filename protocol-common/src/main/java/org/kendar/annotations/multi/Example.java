package org.kendar.annotations.multi;

/**
 * Example of the method
 */
public @interface Example {
    /**
     * Function to call to retrieve the example
     * @return
     */
    String exampleFunction() default "";

    /**
     * Example content
     * @return
     */
    String example() default "";

    /**
     * Example description
     * @return
     */
    String description() default "";
}
