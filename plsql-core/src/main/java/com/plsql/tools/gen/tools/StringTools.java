package com.plsql.tools.gen.tools;

public class StringTools {
    public static Character toChar(String str) {
        return str != null ? str.charAt(0) : null;
    }

    public static String toString(Character str) {
        return str != null ? str.toString() : null;
    }

}
