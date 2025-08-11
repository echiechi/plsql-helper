package com.plsql.tools.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRecord {
    private Long customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Integer age;
    private BigDecimal creditLimit;
    private BigDecimal accountBalance;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private LocalDate dateOfBirth;
    private LocalTime registrationDate;
    private LocalDateTime lastLogin;
    private Boolean isActive;
    private Boolean isPremium;
}