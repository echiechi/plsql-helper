package com.plsql.tools.annotations;

import com.plsql.tools.enums.OutputType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface Output {
    String value() default "p_curs";
    String field() default "";
}
