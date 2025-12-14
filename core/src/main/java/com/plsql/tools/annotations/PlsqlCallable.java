package com.plsql.tools.annotations;

import com.plsql.tools.enums.CallableType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface PlsqlCallable {
    String name() default "";

    String dataSource();

    Output[] outputs() default {};

    CallableType type() default CallableType.PROCEDURE;

}
