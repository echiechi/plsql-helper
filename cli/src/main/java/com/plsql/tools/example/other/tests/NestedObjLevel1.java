package com.plsql.tools.example.other.tests;

import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.math.BigDecimal;

@Record
@Data
public class NestedObjLevel1 {
    private long nestedObjLevel1Element1;
    private BigDecimal nestedObjLevel1Element2;
    private NestedObjectLevel2 nestedObjectLevel2;
}
