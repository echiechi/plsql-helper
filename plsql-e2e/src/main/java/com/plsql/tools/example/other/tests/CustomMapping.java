package com.plsql.tools.example.other.tests;

import com.plsql.tools.example.customer.CustomerMulti;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
public class CustomMapping {

    private String p_customer_id;
    public static String map(String field){
        switch (field){
            case CustomerMulti.Fields.customerGets : return "field_number_1";
            case CustomerMulti.Fields.customerTotal: return "field_number_2";
            default: throw new IllegalStateException("Unknown Field Exception");
        }
    }
}