package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.PlsqlParam;
import com.plsql.tools.annotations.Record;
import lombok.Data;

import java.time.LocalDate;
@Record
@Data
public class ComposedCustomerInsert {
    @PlsqlParam("p_first_name")
    private String firstName;
    @PlsqlParam("p_last_name")
    private String lastName;
    @PlsqlParam("p_email")
    private String email;
    @PlsqlParam("p_phone")
    private String phone;                // default: null
    @PlsqlParam("p_age")
    private Integer age;                 // default: null
    @PlsqlParam("p_credit_limit")
    private Double creditLimit = 1000.00;
    @PlsqlParam("p_account_balance")
    private Double accountBalance = 0.0;
    @PlsqlParam("p_date_of_birth")
    private LocalDate dateOfBirth;       // default: null
    @PlsqlParam("p_is_premium")
    private char isPremium = 'N';
    CustomerAddress customerAddress;
}
