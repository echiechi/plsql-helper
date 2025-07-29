package com.plsql.tools.utils;

public class PlsqlNameValidator {
    private static final String[] RESERVED_KEYWORDS = {
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "NULL", "IS",
            "CREATE", "TABLE", "INSERT", "UPDATE", "DELETE", "BEGIN", "END",
            "IF", "THEN", "LOOP", "WHILE", "FUNCTION", "PROCEDURE", "RETURN",
            "EXCEPTION", "IN", "OUT", "INOUT"
    };

    public static boolean isValidPlsqlName(String name) {
        if (name == null || name.isEmpty() || name.length() > 30) {
            return false;
        }
        // Must start with a letter
        if (!Character.isLetter(name.charAt(0))) {
            return false;
        }
        // Must contain only valid characters
        for (char ch : name.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '$' && ch != '#') {
                return false;
            }
        }
        // Optional: Check against Oracle reserved words (basic example)
        String upperName = name.toUpperCase();
        for (String keyword : RESERVED_KEYWORDS) {
            if (upperName.equals(keyword)) {
                return false;
            }
        }
        return true;
    }

}