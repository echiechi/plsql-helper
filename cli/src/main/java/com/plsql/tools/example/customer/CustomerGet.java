package com.plsql.tools.example.customer;

import com.plsql.tools.annotations.Output;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Data
@Output("p_customer_data")
public class CustomerGet {
    private Long customerId;                // NUMBER(10,0)
    private Integer age;                    // NUMBER(3,0)
    private BigDecimal creditLimit;         // NUMBER(10,2)
    private Double accountBalance;          // BINARY_DOUBLE
    private String firstName;               // VARCHAR2(50) NOT NULL
    private String lastName;                // VARCHAR2(50) NOT NULL
    private String email;                   // VARCHAR2(100)
    private String phone;                   // CHAR(15)
    private String addressLine1;            // VARCHAR2(200)
    private String addressLine2;            // VARCHAR2(200)
    private String city;                    // VARCHAR2(50)
    private String stateProvince;           // VARCHAR2(50)
    private String postalCode;              // VARCHAR2(20)
    private String country = "USA";         // VARCHAR2(50) DEFAULT 'USA'
    private LocalDate dateOfBirth;          // DATE
    private LocalDateTime registrationDate; // TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
    private LocalTime lastLogin;       // TIMESTAMP(6) WITH TIME ZONE
    private char isActive = 'Y';            // CHAR(1) DEFAULT 'Y'
    private char isPremium = 'N';
}
