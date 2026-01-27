package com.plsql.tools.gen.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTools {

    public static java.sql.Date toSqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return java.sql.Date.valueOf(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }

    public static java.sql.Date toSqlDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return java.sql.Date.valueOf(date);
    }

    public static java.sql.Timestamp toTimestamp(java.time.LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return java.sql.Timestamp.valueOf(date);
    }

    public static java.sql.Time toTime(java.time.LocalTime date) {
        if (date == null) {
            return null;
        }
        return java.sql.Time.valueOf(date);
    }

    public static LocalDate toLocalDate(java.sql.Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    public static java.util.Date toDate(java.sql.Date date) {
        return date != null ? new java.util.Date(date.getTime()) : null;
    }

    public static LocalDateTime toLocalDateTime(java.sql.Timestamp date) {
        return date != null ? date.toLocalDateTime() : null;
    }

    public static LocalTime toLocalTime(java.sql.Time time) {
        return time != null ? time.toLocalTime() : null;
    }

}
