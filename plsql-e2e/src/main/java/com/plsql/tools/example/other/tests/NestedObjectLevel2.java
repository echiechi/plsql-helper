package com.plsql.tools.example.other.tests;

import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.math.BigDecimal;

@Record
@Data
public class NestedObjectLevel2 {
    private long nestedObjLevel2Element1;
    private BigDecimal nestedObjLevel2Element2;
}
