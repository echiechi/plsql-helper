package com.plsql.tools.tools;

import static com.plsql.tools.utils.CaseConverter.upperCaseFirstLetter;

public class CodeGenConstants {
    // Variable naming
    public static final String VARIABLE_SUFFIX = "__$";
    public static final String POSITION_VAR = "pos";
    public static final String RESULT_SET_VAR = "rs";
    public static final String STATEMENT_VAR = "stmt";
    public static final String RETURN_VAR = "result";

    public static final String EMPTY_METHOD = "empty()";

    // Method prefixes
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";
    public static final String IS_PREFIX = "is";

    public static String variableName(String name) {
        return name + VARIABLE_SUFFIX;
    }

    public static String wrappedVariableName(String defaultReturnName) {
        return "wrapped%s".formatted(variableName(upperCaseFirstLetter(defaultReturnName)));
    }
}
