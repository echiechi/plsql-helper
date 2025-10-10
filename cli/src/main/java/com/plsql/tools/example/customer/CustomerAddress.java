package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Input;
import com.plsql.tools.annotations.Record;
import lombok.Data;

@Record
@Data
public class CustomerAddress {
    @Input("p_address_line1")
    private String addressLine1;         // default: null
    @Input("p_address_line2")
    private String addressLine2;         // default: null
    @Input("p_city")
    private String city;                 // default: null
    @Input("p_state_province")
    private String stateProvince;        // default: null
    @Input("p_postal_code")
    private String postalCode;           // default: null
    @Input("p_country")
    private String country = "USA";
}
