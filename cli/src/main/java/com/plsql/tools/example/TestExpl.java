package com.plsql.tools.example;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;


public class TestExpl {
    @Getter
    @Setter
    private String test1;

    @Getter
    @Setter
    private String test2;

    public int test3;

    public Integer test4;

    public BigDecimal test5;
    public Date test6;

    public LocalDate test7;
}
