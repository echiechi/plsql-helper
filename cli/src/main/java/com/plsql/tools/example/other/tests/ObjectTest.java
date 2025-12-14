package com.plsql.tools.example.other.tests;

import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Record
@Data
public class ObjectTest {
    private String element1;
    private String element2;
    private Date testDate;
    private LocalDate testLocalDate;
    private LocalDateTime testDateTime;
    private java.time.LocalTime localTimeTest;
    private NestedObjLevel1 nestedObjLevel1;
    private NestedObjLevel1 nestedObjLevel1Second;
}
