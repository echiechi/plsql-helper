package com.plsql.tools.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a PL/SQL function call.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Function {
    String name() default "";

    Class<?>[] inputs() default {};

    Class<?>[] output() default Integer.class;
}
