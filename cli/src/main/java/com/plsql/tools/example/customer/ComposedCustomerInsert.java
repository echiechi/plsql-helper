package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Input;
import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.time.LocalDate;
@Record
@Data
public class ComposedCustomerInsert {
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
    @Input("p_date_of_birth")
    private LocalDate dateOfBirth;       // default: null
    @Input("p_is_premium")
    private char isPremium = 'N';
    CustomerAddress customerAddress;
}
