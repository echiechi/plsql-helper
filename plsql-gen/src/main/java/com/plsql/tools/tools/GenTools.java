package com.plsql.tools.tools;

import com.plsql.tools.tools.extraction.info.ElementInfo;

import java.util.Optional;

import static com.plsql.tools.tools.CodeGenConstants.variableName;

public class GenTools {

    // example:  Type name = new Type();
    public static String initObject(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        if (typeInfo.isWrapped()) {
            return GenTools.assignAndInitObject(typeInfo.wrappedTypeAsString(), variableName(elementInfo.getName()));
        }
        return GenTools.assignAndInitObject(typeInfo.typeAsString(), variableName(elementInfo.getName()));
    }

    public static String incrementVar(String name) {
        return String.format("%s++", name);
    }

    public static String preIncrementVar(String name) {
        return String.format("++%s", name);
    }

    public static String preDecrementVar(String name) {
        return String.format("--%s", name);
    }

    public static String joinWithDot(String... args) {
        return String.join(".", args);
    }

    public static String joinWithComma(String... args) {
        return String.join(",", args);
    }

    public static String join(String... toAppend) {
        return String.join("", toAppend);
    }

    public static String literalString(String parameterName) {
        return join("\"", parameterName, "\"");
    }

    public static String appendGetter(String object, String getterName) {
        return constructMethod(object, getterName);
    }

    public static String addToCollection(String collectionName, String objectName) {
        return "%s.add(%s);".formatted(collectionName, objectName);
    }

    public static String returnObject(String toReturn) {
        return "return %s;".formatted(toReturn);
    }

    public static String collectionInit(String interfaceType, String objectType, String name, String collectionType) {
        return "%s %s = new %s<>();".formatted(genericType(interfaceType, objectType), name, collectionType);
    }

    public static String optionalAssign(String optionalName, String objectName) {
        return "%s = %s.of(%s);".formatted(optionalName, Optional.class.getCanonicalName(), objectName);
    }

    public static String optionalInit(String objectType, String name) {
        return optionalInit(
                Optional.class.getCanonicalName(),
                objectType,
                name,
                Optional.class.getCanonicalName());
    }

    public static String optionalInit(String interfaceType, String objectType, String name, String collectionType) {
        return "%s %s = %s.empty();".formatted(genericType(interfaceType, objectType), name, collectionType);
    }

    public static String genericType(String type, String genericType) {
        return "%s<%s>".formatted(type, genericType);
    }

    public static String constructMethod(String objectName, String methodName, String... paramNames) {
        return join(joinWithDot(objectName, methodName), "(", joinWithComma(paramNames), ")");
    }

    public static String assignNullAndInit(String type, String objectName) {
        return assignAndInit(type, objectName, "null");
    }

    public static String assignAndInit(String type, String objectName, String value) {
        return join(type, " ", assign(objectName, value));
    }

    public static String assignAndInitObject(String type, String objectName, String... paramNames) {
        return join(type, " ", assign(objectName, newObject(type, paramNames)));
    }

    public static String assignNewObject(String type, String objectName, String... paramNames) {
        return assign(objectName, newObject(type, paramNames));
    }

    public static String assign(String objectName, String value) {
        return join(objectName, " = ", value, ";");
    }

    public static String newObject(String type, String... paramNames) {
        return join("new ", type, "(", joinWithComma(paramNames), ")");
    }

    public static String charToString(String name) {
        return "StringTools.toString(%s)".formatted(name);
    }

    public static String toChar(String name) {
        return "StringTools.toChar(%s)".formatted(name);
    }

    public static String toSqlDate(String name) {
        return "DateTools.toSqlDate(%s)".formatted(name);
    }

    public static String toTimestamp(String name) {
        return "DateTools.toTimestamp(%s)".formatted(name);
    }

    public static String toTime(String name) {
        return "DateTools.toTime(%s)".formatted(name);
    }

    public static String toLocalDate(String name) {
        return "DateTools.toLocalDate(%s)".formatted(name);
    }

    public static String toLocalDateTime(String name) {
        return "DateTools.toLocalDateTime(%s)".formatted(name);
    }

    public static String toLocalTime(String name) {
        return "DateTools.toLocalTime(%s)".formatted(name);
    }

    public static String toDate(String name) {
        return "DateTools.toDate(%s)".formatted(name);
    }

    public static String registerOutParameter(String stmt, String pos, String jdbcType) {
        return String.format("%s.registerOutParameter(%s, JDBCType.%s);",
                stmt, pos, jdbcType
        );
    }
}
