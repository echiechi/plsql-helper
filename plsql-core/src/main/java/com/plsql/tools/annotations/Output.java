package com.plsql.tools.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface Output {
    String value() default "p_curs";
    String field() default "";
}
