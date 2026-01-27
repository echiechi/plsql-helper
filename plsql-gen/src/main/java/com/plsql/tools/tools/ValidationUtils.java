package com.plsql.tools.tools;

import static com.plsql.tools.tools.CodeGenConstants.*;

public class ValidationUtils {

    public static boolean isValidGetter(String methodName) {
        return methodName != null &&
                (methodName.startsWith(GETTER_PREFIX) || methodName.startsWith(IS_PREFIX));
    }

    public static boolean isValidSetter(String methodName) {
        return methodName != null && methodName.startsWith(SETTER_PREFIX);
    }
}
