package com.plsql.tools.example.other.tests;

import com.plsql.tools.example.customer.CustomerMulti;

public class CustomMapping {
    public static String map(String field){
        switch (field){
            case CustomerMulti.Fields.customerGets : return "field_number_1";
            case CustomerMulti.Fields.customerTotal: return "field_number_2";
            default: throw new IllegalStateException("Unkown Field Exception");
        }
    }
}
