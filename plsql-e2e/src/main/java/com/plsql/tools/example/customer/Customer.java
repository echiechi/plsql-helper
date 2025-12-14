package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Record;
import com.plsql.tools.example.other.tests.NestedObjLevel1;

@Record
public record Customer(String firstName, NestedObjLevel1 nestedObjLevel1) {
}
