package com.plsql.tools.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.JDBCType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enum representing common Java types with their Class objects, JDBC types, and utility methods
 */
public enum JavaToSql {
    // Primitive types
    BYTE(byte.class, "setByte", "byte", JDBCType.TINYINT),
    SHORT(short.class, "setShort", "short", JDBCType.SMALLINT),
    INTEGER(int.class, "setInt", "int", JDBCType.INTEGER),
    LONG(long.class, "setLong", "long", JDBCType.BIGINT),
    FLOAT(float.class, "setFloat", "float", JDBCType.REAL),
    DOUBLE(double.class, "setDouble", "double", JDBCType.DOUBLE),
    BOOLEAN(boolean.class, "setBoolean", "boolean", JDBCType.BOOLEAN),
    CHARACTER(char.class, "setString", "char", JDBCType.CHAR),

    // Wrapper classes
    BYTE_WRAPPER(Byte.class, "setByte", "java.lang.Byte", JDBCType.TINYINT),
    SHORT_WRAPPER(Short.class, "setShort", "java.lang.Short", JDBCType.SMALLINT),
    INTEGER_WRAPPER(Integer.class, "setInt", "java.lang.Integer", JDBCType.INTEGER),
    LONG_WRAPPER(Long.class, "setLong", "java.lang.Long", JDBCType.BIGINT),
    FLOAT_WRAPPER(Float.class, "setFloat", "java.lang.Float", JDBCType.REAL),
    DOUBLE_WRAPPER(Double.class, "setDouble", "java.lang.Double", JDBCType.DOUBLE),
    BOOLEAN_WRAPPER(Boolean.class, "setBoolean", "java.lang.Boolean", JDBCType.BOOLEAN),
    CHARACTER_WRAPPER(Character.class, "setString", "java.lang.Character", JDBCType.CHAR),

    // Common object types
    STRING(String.class, "setString", "java.lang.String", JDBCType.VARCHAR),
    OBJECT(Object.class, "setObject", "java.lang.Object", JDBCType.JAVA_OBJECT),

    // Math types
    BIG_DECIMAL(BigDecimal.class, "setBigDecimal", "java.math.BigDecimal", JDBCType.DECIMAL),
    BIG_INTEGER(BigInteger.class, "setBigDecimal", "java.math.BigInteger", JDBCType.NUMERIC),

    // Date/Time types
    DATE(Date.class, "setDate", "java.util.Date", JDBCType.TIMESTAMP),
    LOCAL_DATE(LocalDate.class, "setDate", "java.time.LocalDate", JDBCType.DATE),
    LOCAL_TIME(LocalTime.class, "setTime", "java.time.LocalTime", JDBCType.TIME),
    LOCAL_DATE_TIME(LocalDateTime.class, "setTimestamp", "java.time.LocalDateTime", JDBCType.TIMESTAMP),

    // Collection types (no direct JDBC setter, use setObject)
    LIST(List.class, "setObject", "java.util.List", JDBCType.JAVA_OBJECT),
    SET(Set.class, "setObject", "java.util.Set", JDBCType.JAVA_OBJECT),
    MAP(Map.class, "setObject", "java.util.Map", JDBCType.JAVA_OBJECT),

    // Array types
    BYTE_ARRAY(byte[].class, "setBytes", "byte[]", JDBCType.VARBINARY),
    STRING_ARRAY(String[].class, "setArray", "java.lang.String[]", JDBCType.ARRAY),
    OBJECT_ARRAY(Object[].class, "setArray", "java.lang.Object[]", JDBCType.ARRAY);

    private final Class<?> primitiveType;
    private final String jdbcSetterMethod;
    private final String displayName;
    private final JDBCType jdbcType;

    JavaToSql(Class<?> primitiveType, String jdbcSetterMethod, String displayName, JDBCType jdbcType) {
        this.primitiveType = primitiveType;
        this.jdbcSetterMethod = jdbcSetterMethod;
        this.displayName = displayName;
        this.jdbcType = jdbcType;
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

    public JDBCType getJdbcType() {
        return jdbcType;
    }

    public int getSqlType() {
        return jdbcType.getVendorTypeNumber();
    }

    public String getSqlTypeName() {
        return jdbcType.getName();
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
     * Find JavaToSql by Class
     */
    public static JavaToSql fromClass(Class<?> clazz) {
        for (JavaToSql type : values()) {
            if (type.primitiveType.equals(clazz)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaToSql by simple class name
     */
    public static JavaToSql fromSimpleName(String simpleName) {
        for (JavaToSql type : values()) {
            if (type.displayName.equalsIgnoreCase(simpleName) ||
                    type.primitiveType.getSimpleName().equalsIgnoreCase(simpleName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaToSql by JDBC setter method name
     */
    public static JavaToSql fromJdbcSetter(String setterMethod) {
        for (JavaToSql type : values()) {
            if (type.jdbcSetterMethod.equalsIgnoreCase(setterMethod)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaToSql by JDBC type
     */
    public static JavaToSql fromJdbcType(JDBCType jdbcType) {
        for (JavaToSql type : values()) {
            if (type.jdbcType == jdbcType) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaToSql by SQL type constant
     */
    public static JavaToSql fromSqlType(int sqlType) {
        for (JavaToSql type : values()) {
            if (type.jdbcType.getVendorTypeNumber() == sqlType) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JavaToSql by SQL type name
     */
    public static JavaToSql fromSqlTypeName(String sqlTypeName) {
        for (JavaToSql type : values()) {
            if (type.jdbcType.getName().equalsIgnoreCase(sqlTypeName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get all numeric types
     */
    public static JavaToSql[] getNumericTypes() {
        return new JavaToSql[] {
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_DECIMAL, BIG_INTEGER,
                BYTE_WRAPPER, SHORT_WRAPPER, INTEGER_WRAPPER, LONG_WRAPPER,
                FLOAT_WRAPPER, DOUBLE_WRAPPER
        };
    }

    /**
     * Get all primitive types
     */
    public static JavaToSql[] getPrimitiveTypes() {
        return new JavaToSql[] {
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, CHARACTER
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}