package com.plsql.tools.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Procedure {
    String name() default "";
    String dataSource();
    Class<?>[] outputs() default {};

    Class<?> input() default Object.class;
}
