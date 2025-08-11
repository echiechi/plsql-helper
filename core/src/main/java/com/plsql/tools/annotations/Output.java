package com.plsql.tools.annotations;

import java.sql.ResultSet;

public @interface Output {
    String name() default "p_crus";

    Class<?> type() default java.sql.ResultSet.class;
}
