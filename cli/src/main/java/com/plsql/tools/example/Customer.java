package com.plsql.tools.example;

import com.plsql.tools.annotations.Param;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private String pLastName;
    @Param("p_email")
    private String myEmail;
    private String pCity;
    private char pIsActive;
}
