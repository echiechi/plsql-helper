package com.plsql.tools.annotations;

public @interface InnerOutput {
    String value() default "";
    String field() default "";
}
