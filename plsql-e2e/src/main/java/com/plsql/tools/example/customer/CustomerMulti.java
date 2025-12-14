package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Record;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Record
@FieldNameConstants
public class CustomerMulti {
    private long customerTotal;
    private List<CustomerGet> customerGets;
}
