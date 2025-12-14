package com.plsql.tools.gen.tools;

import java.time.LocalDate;

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

}
