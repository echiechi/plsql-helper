package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.PlsqlParam;
import com.plsql.tools.annotations.Record;
import lombok.Data;

@Record
@Data
public class CustomerAddress {
    @PlsqlParam("p_address_line1")
    private String addressLine1;         // default: null
    @PlsqlParam("p_address_line2")
    private String addressLine2;         // default: null
    @PlsqlParam("p_city")
    private String city;                 // default: null
    @PlsqlParam("p_state_province")
    private String stateProvince;        // default: null
    @PlsqlParam("p_postal_code")
    private String postalCode;           // default: null
    @PlsqlParam("p_country")
    private String country = "USA";
}
