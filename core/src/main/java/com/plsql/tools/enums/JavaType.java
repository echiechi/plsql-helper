package com.plsql.tools.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enum representing common Java types with their Class objects and utility methods
 */
public enum JavaType {
    // Primitive types
    BYTE(byte.class, "setByte", "byte"),
    SHORT(short.class, "setShort", "short"),
    INTEGER(int.class, "setInt", "int"),
    LONG(long.class, "setLong", "long"),
    FLOAT(float.class, "setFloat", "float"),
    DOUBLE(double.class, "setDouble", "double"),
    BOOLEAN(boolean.class, "setBoolean", "boolean"),
    CHARACTER(char.class, "setString", "char"),

    // Wrapper classes
    BYTE_WRAPPER(Byte.class, "setByte", "java.lang.Byte"),
    SHORT_WRAPPER(Short.class, "setShort", "java.lang.Short"),
    INTEGER_WRAPPER(Integer.class, "setInt", "java.lang.Integer"),
    LONG_WRAPPER(Long.class, "setLong", "java.lang.Long"),
    FLOAT_WRAPPER(Float.class, "setFloat", "java.lang.Float"),
    DOUBLE_WRAPPER(Double.class, "setDouble", "java.lang.Double"),
    BOOLEAN_WRAPPER(Boolean.class, "setBoolean", "java.lang.Boolean"),
    CHARACTER_WRAPPER(Character.class, "setString", "java.lang.Character"),

    // Common object types
    STRING(String.class, "setString", "java.lang.String"),
    OBJECT(Object.class, "setObject", "java.lang.Object"),

    // Math types
    BIG_DECIMAL(BigDecimal.class, "setBigDecimal", "java.math.BigDecimal"),
    BIG_INTEGER(BigInteger.class, "setBigDecimal", "java.math.BigInteger"),

    // Date/Time types
    DATE(Date.class, "setDate", "java.util.Date"),
    LOCAL_DATE(LocalDate.class, "setDate", "java.time.LocalDate"),
    LOCAL_TIME(LocalTime.class, "setTime", "java.time.LocalTime"),
    LOCAL_DATE_TIME(LocalDateTime.class, "setTimestamp", "java.time.LocalDateTime"),

    // Collection types (no direct JDBC setter, use setObject)
    LIST(List.class, "setObject", "java.util.List"),
    SET(Set.class, "setObject", "java.util.Set"),
    MAP(Map.class, "setObject", "java.util.Map"),

    // Array types
    BYTE_ARRAY(byte[].class, "setBytes", "byte[]"),
    STRING_ARRAY(String[].class, "setArray", "java.lang.String[]"),
    OBJECT_ARRAY(Object[].class, "setArray", "java.lang.Object[]");

    private final Class<?> primitiveType;
    private final String jdbcSetterMethod;
    private final String displayName;

    JavaType(Class<?> primitiveType, String jdbcSetterMethod, String displayName) {
        this.primitiveType = primitiveType;
        this.jdbcSetterMethod = jdbcSetterMethod;
        this.displayName = displayName;
    }

    public Class<?> getType() {
        return primitiveType;
    }

    public String getJdbcSetterMethod() {
        return jdbcSetterMethod;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPrimitive() {
        return primitiveType.isPrimitive();
    }

    public boolean isNumeric() {
        return this == BYTE || this == SHORT || this == INTEGER || this == LONG ||
                this == FLOAT || this == DOUBLE || this == BIG_DECIMAL || this == BIG_INTEGER ||
                this == BYTE_WRAPPER || this == SHORT_WRAPPER || this == INTEGER_WRAPPER ||
                this == LONG_WRAPPER || this == FLOAT_WRAPPER || this == DOUBLE_WRAPPER;
    }

    public boolean isCollection() {
        return this == LIST || this == SET || this == MAP;
    }

    public boolean isArray() {
        return this == BYTE_ARRAY || this == STRING_ARRAY || this == OBJECT_ARRAY;
    }

    public boolean isDateTime() {
        return this == DATE || this == LOCAL_DATE || this == LOCAL_TIME || this == LOCAL_DATE_TIME;
    }

    /**
     * Find JavaType by Class
     */
    public static JavaType fromClass(Class<?> clazz) {
        for (JavaType type : values()) {
            if (type.primitiveType.equals(clazz)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaType by simple class name
     */
    public static JavaType fromSimpleName(String simpleName) {
        for (JavaType type : values()) {
            if (type.displayName.equalsIgnoreCase(simpleName) ||
                    type.primitiveType.getSimpleName().equalsIgnoreCase(simpleName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaType by JDBC setter method name
     */
    public static JavaType fromJdbcSetter(String setterMethod) {
        for (JavaType type : values()) {
            if (type.jdbcSetterMethod.equalsIgnoreCase(setterMethod)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get all numeric types
     */
    public static JavaType[] getNumericTypes() {
        return new JavaType[] {
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_DECIMAL, BIG_INTEGER,
                BYTE_WRAPPER, SHORT_WRAPPER, INTEGER_WRAPPER, LONG_WRAPPER,
                FLOAT_WRAPPER, DOUBLE_WRAPPER
        };
    }

    /**
     * Get all primitive types
     */
    public static JavaType[] getPrimitiveTypes() {
        return new JavaType[] {
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, CHARACTER
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}