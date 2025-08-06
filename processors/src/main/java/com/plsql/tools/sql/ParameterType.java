package com.plsql.tools.sql;

public enum ParameterType {
    IN("IN"),
    OUT("OUT"),
    IN_OUT("IN OUT");

    private final String sqlType;

    ParameterType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getSqlType() {
        return sqlType;
    }
}
