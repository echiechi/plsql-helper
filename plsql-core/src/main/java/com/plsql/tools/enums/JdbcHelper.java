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
public enum JdbcHelper {
    // Primitive types
    BYTE(byte.class, "setByte", "getByte", "byte", JDBCType.TINYINT),
    SHORT(short.class, "setShort", "getShort", "short", JDBCType.SMALLINT),
    INTEGER(int.class, "setInt", "getInt", "int", JDBCType.INTEGER),
    LONG(long.class, "setLong", "getLong", "long", JDBCType.BIGINT),
    FLOAT(float.class, "setFloat", "getFloat", "float", JDBCType.REAL),
    DOUBLE(double.class, "setDouble", "getDouble", "double", JDBCType.DOUBLE),
    BOOLEAN(boolean.class, "setBoolean", "getBoolean", "boolean", JDBCType.BOOLEAN),
    CHARACTER(char.class, "setString", "getString", "char", JDBCType.CHAR),

    // Wrapper classes
    BYTE_WRAPPER(Byte.class, "setByte", "getByte", "java.lang.Byte", JDBCType.TINYINT),
    SHORT_WRAPPER(Short.class, "setShort", "getShort", "java.lang.Short", JDBCType.SMALLINT),
    INTEGER_WRAPPER(Integer.class, "setInt", "getInt", "java.lang.Integer", JDBCType.INTEGER),
    LONG_WRAPPER(Long.class, "setLong", "getLong", "java.lang.Long", JDBCType.BIGINT),
    FLOAT_WRAPPER(Float.class, "setFloat", "getFloat", "java.lang.Float", JDBCType.REAL),
    DOUBLE_WRAPPER(Double.class, "setDouble", "getDouble", "java.lang.Double", JDBCType.DOUBLE),
    BOOLEAN_WRAPPER(Boolean.class, "setBoolean", "getBoolean", "java.lang.Boolean", JDBCType.BOOLEAN),
    CHARACTER_WRAPPER(Character.class, "setString", "getString", "java.lang.Character", JDBCType.CHAR),

    // Common object types
    STRING(String.class, "setString", "getString", "java.lang.String", JDBCType.VARCHAR),
    OBJECT(Object.class, "setObject", "getObject", "java.lang.Object", JDBCType.JAVA_OBJECT),

    // Math types
    BIG_DECIMAL(BigDecimal.class, "setBigDecimal", "getBigDecimal", "java.math.BigDecimal", JDBCType.DECIMAL),
    BIG_INTEGER(BigInteger.class, "setBigDecimal", "getBigDecimal", "java.math.BigInteger", JDBCType.NUMERIC),

    // Date/Time types
    DATE(Date.class, "setDate", "getDate", "java.util.Date", JDBCType.TIMESTAMP),
    LOCAL_DATE(LocalDate.class, "setDate", "getDate", "java.time.LocalDate", JDBCType.DATE),
    LOCAL_TIME(LocalTime.class, "setTime", "getTime", "java.time.LocalTime", JDBCType.TIME),
    LOCAL_DATE_TIME(LocalDateTime.class, "setTimestamp", "getTimestamp", "java.time.LocalDateTime", JDBCType.TIMESTAMP),

    // Collection types (no direct JDBC setter, use setObject)
    LIST(List.class, "setObject", "getObject", "java.util.List", JDBCType.JAVA_OBJECT),
    SET(Set.class, "setObject", "getObject", "java.util.Set", JDBCType.JAVA_OBJECT),
    MAP(Map.class, "setObject", "getObject", "java.util.Map", JDBCType.JAVA_OBJECT),

    // Array types
    BYTE_ARRAY(byte[].class, "setBytes", "getBytes", "byte[]", JDBCType.VARBINARY),
    STRING_ARRAY(String[].class, "setArray", "getArray", "java.lang.String[]", JDBCType.ARRAY),
    OBJECT_ARRAY(Object[].class, "setArray", "getArray", "java.lang.Object[]", JDBCType.ARRAY);
    // NULL(null, "setNull", "getNull", "null", JDBCType.NULL);
    private final Class<?> primitiveType;
    private final String jdbcSetterMethod;
    private final String jdbcGetterMethod;
    private final String displayName;
    private final JDBCType jdbcType;

    JdbcHelper(Class<?> primitiveType, String jdbcSetterMethod, String jdbcGetterMethod, String displayName, JDBCType jdbcType) {
        this.primitiveType = primitiveType;
        this.jdbcSetterMethod = jdbcSetterMethod;
        this.jdbcGetterMethod = jdbcGetterMethod;
        this.displayName = displayName;
        this.jdbcType = jdbcType;
    }

    public Class<?> getType() {
        return primitiveType;
    }

    public String getJdbcSetterMethod() {
        return jdbcSetterMethod;
    }

    public String getJdbcGetterMethod() {
        return jdbcGetterMethod;
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
     * Find JdbcHelper by Class
     */
    public static JdbcHelper fromClass(Class<?> clazz) {
        for (JdbcHelper type : values()) {
            if (type.primitiveType.equals(clazz)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by simple class name
     */
    public static JdbcHelper fromSimpleName(String simpleName) {
        for (JdbcHelper type : values()) {
            if (type.displayName.equalsIgnoreCase(simpleName) ||
                    type.primitiveType.getSimpleName().equalsIgnoreCase(simpleName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by JDBC setter method name
     */
    public static JdbcHelper fromJdbcSetter(String setterMethod) {
        for (JdbcHelper type : values()) {
            if (type.jdbcSetterMethod.equalsIgnoreCase(setterMethod)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by JDBC getter method name
     */
    public static JdbcHelper fromJdbcGetter(String getterMethod) {
        for (JdbcHelper type : values()) {
            if (type.jdbcGetterMethod.equalsIgnoreCase(getterMethod)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by JDBC type
     */
    public static JdbcHelper fromJdbcType(JDBCType jdbcType) {
        for (JdbcHelper type : values()) {
            if (type.jdbcType == jdbcType) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by SQL type constant
     */
    public static JdbcHelper fromSqlType(int sqlType) {
        for (JdbcHelper type : values()) {
            if (type.jdbcType.getVendorTypeNumber() == sqlType) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find JdbcHelper by SQL type name
     */
    public static JdbcHelper fromSqlTypeName(String sqlTypeName) {
        for (JdbcHelper type : values()) {
            if (type.jdbcType.getName().equalsIgnoreCase(sqlTypeName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get all numeric types
     */
    public static JdbcHelper[] getNumericTypes() {
        return new JdbcHelper[]{
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_DECIMAL, BIG_INTEGER,
                BYTE_WRAPPER, SHORT_WRAPPER, INTEGER_WRAPPER, LONG_WRAPPER,
                FLOAT_WRAPPER, DOUBLE_WRAPPER
        };
    }

    /**
     * Get all primitive types
     */
    public static JdbcHelper[] getPrimitiveTypes() {
        return new JdbcHelper[]{
                BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, CHARACTER
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}