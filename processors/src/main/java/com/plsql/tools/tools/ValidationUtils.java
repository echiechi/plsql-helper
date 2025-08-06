package com.plsql.tools.tools;

public class ValidationUtils {

    private static final String GETTER_PREFIX_GET = "get";
    private static final String GETTER_PREFIX_IS = "is";

    public static boolean isValidGetter(String methodName) {
        return methodName != null &&
                (methodName.startsWith(GETTER_PREFIX_GET) || methodName.startsWith(GETTER_PREFIX_IS));
    }

    public static boolean isValidMethodName(String methodName) {
        return methodName != null &&
                !methodName.trim().isEmpty() &&
                methodName.matches("[a-zA-Z][a-zA-Z0-9_]*");
    }

    public static boolean isValidFieldName(String fieldName) {
        return fieldName != null &&
                !fieldName.trim().isEmpty() &&
                fieldName.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
}
