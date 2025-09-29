package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Input;
import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.time.LocalDate;

@Data
@Record
public class CustomerInsert {
    @Input("p_first_name")
    private String firstName;
    @Input("p_last_name")
    private String lastName;
    @Input("p_email")
    private String email;
    @Input("p_phone")
    private String phone;                // default: null
    @Input("p_age")
    private Integer age;                 // default: null
    @Input("p_credit_limit")
    private Double creditLimit = 1000.00;
    @Input("p_account_balance")
    private Double accountBalance = 0.0;
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
    @Input("p_date_of_birth")
    private LocalDate dateOfBirth;       // default: null
    @Input("p_is_premium")
    private char isPremium = 'N';
}
