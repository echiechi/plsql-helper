package com.plsql.tools.utils;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TypeHelper {
    public static int inferSqlType(Class<?> type) {
        if (type == String.class) return Types.VARCHAR;
        if (type == Integer.class || type == int.class) return Types.INTEGER;
        if (type == Long.class || type == long.class) return Types.BIGINT;
        if (type == Boolean.class || type == boolean.class) return Types.BOOLEAN;
        if (type == Double.class || type == double.class) return Types.DOUBLE;
        if (type == Float.class || type == float.class) return Types.FLOAT;
        if (type == java.sql.Date.class) return Types.DATE;
        if (type == java.sql.Time.class) return Types.TIME;
        if (type == java.sql.Timestamp.class) return Types.TIMESTAMP;
        if (type == LocalDate.class) return Types.DATE;
        if (type == LocalTime.class) return Types.TIME;
        if (type == LocalDateTime.class) return Types.TIMESTAMP;
        return Types.OTHER;
    }

    public static Class<?> inferJavaType(int type) {
        return switch (type) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB ->
                    String.class;
            case Types.INTEGER, Types.SMALLINT -> Integer.class;
            case Types.BIGINT -> Long.class;
            case Types.TINYINT -> Byte.class;
            case Types.BOOLEAN, Types.BIT -> Boolean.class;
            case Types.DECIMAL, Types.NUMERIC -> java.math.BigDecimal.class;
            case Types.DOUBLE, Types.FLOAT -> // FLOAT in SQL is typically double precision
                    Double.class;
            case Types.REAL -> Float.class;
            case Types.DATE -> java.sql.Date.class;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> java.sql.Time.class;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> java.sql.Timestamp.class;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> byte[].class;
            case Types.ARRAY -> java.sql.Array.class;
            case Types.REF, Types.REF_CURSOR -> java.sql.Ref.class;
            case Types.STRUCT -> java.sql.Struct.class;
            case Types.DATALINK -> java.net.URL.class;
            case Types.SQLXML -> java.sql.SQLXML.class;
            case Types.ROWID -> java.sql.RowId.class;
            case Types.JAVA_OBJECT -> Object.class;
            default -> Object.class;
        };
    }
}