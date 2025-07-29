package com.plsql.tools.example;

import lombok.Getter;

@Getter
public class Customer {
    private String pLastName;
    private String pEmail;
    private String pCity;
    private char pIsActive; // TODO: handle char correctly
}
