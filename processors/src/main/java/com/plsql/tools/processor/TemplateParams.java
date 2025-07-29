package com.plsql.tools.processor;

public enum TemplateParams {
    PACKAGE_NAME("packageName"),
    CLASS_NAME("className"),
    EXTENDED_CLASS_NAME("extendedClassName"),
    RETURN_TYPE("returnType"),
    METHOD_NAME("methodName"),
    PARAMETERS("parameters"),
    METHODS("methods"),
    PROCEDURE_FULL_NAME("procedureFullName"),
    PROCEDURE_CALL_NAME("procedureCallName"),
    PROCEDURE_PARAMETERS("procedureParameters"),
    STATEMENT_STATIC_CALL("statementStaticCall"),
    DATA_SOURCE("dataSource"),
    STATEMENT_POPULATION("statementPopulation");
    private final String value;

    TemplateParams(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
