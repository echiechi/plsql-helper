package com.plsql.tools;


import com.plsql.tools.example.customer.*;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Random;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static final String url = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";
    public static final String userName = "SYSTEM";
    public static final String password = "admin";

    public static void main(String[] args) throws SQLException {
        var connectionProperties = new Properties();
        connectionProperties.setProperty(OracleConnection.CONNECTION_PROPERTY_USER_NAME, userName);
        connectionProperties.setProperty(OracleConnection.CONNECTION_PROPERTY_PASSWORD, password);

        var oracleDataSource = new OracleDataSource();
        oracleDataSource.setConnectionProperties(connectionProperties);
        oracleDataSource.setURL(url);

        var dsProvider = new DefaultDataSourceProvider();
        dsProvider.registerDataSource("MY_DS", oracleDataSource);

        CustomerService customerService = new CustomerServiceImpl(dsProvider);
        CustomerService2 customerService2 = new CustomerService2Impl(dsProvider);

        CustomerInsert customerInsert = initRandomCustomer();
        System.out.println(customerService.insertCustomerObject(
                customerInsert.getFirstName(),
                customerInsert.getLastName(),
                customerInsert.getEmail(),
                customerInsert.getPhone(),
                customerInsert.getAge(),
                customerInsert.getCreditLimit(),
                customerInsert.getAccountBalance(),
                customerInsert.getAddressLine1(),
                customerInsert.getAddressLine2(),
                customerInsert.getCity(),
                customerInsert.getStateProvince(),
                customerInsert.getPostalCode(),
                customerInsert.getCountry(),
                customerInsert.getDateOfBirth(),
                customerInsert.getIsPremium()
        ));

        long customerId = customerService2.insertCustomerObject(initRandomCustomer());

        customerService.displayMessageCustomer();

        System.out.println(customerService.getCustomerById(customerId));
    }

    public static CustomerInsert initRandomCustomer() {
        Random random = new Random();
        CustomerInsert customer = new CustomerInsert(); // 👈 using default constructor
        customer.setFirstName("Alice");
        customer.setLastName("Smith");
        customer.setEmail("alice.smith." + (1000 + random.nextInt(9000)) + "@example.com");
        customer.setPhone("+1-555-" + (1000 + random.nextInt(9000)));
        customer.setAge(18 + random.nextInt(50)); // random age 18–67
        customer.setCreditLimit(500.0 + random.nextDouble() * 5000); // 500–5500
        customer.setAccountBalance(random.nextDouble() * 10000); // 0–9999
        customer.setAddressLine1("456 Elm Street");
        customer.setAddressLine2("Unit " + (1 + random.nextInt(100)));
        customer.setCity("Los Angeles");
        customer.setStateProvince("CA");
        customer.setPostalCode("90" + (100 + random.nextInt(900)));
        customer.setCountry("USA"); // default anyway, but you can override
        customer.setDateOfBirth(LocalDate.of(
                1950 + random.nextInt(50), // year 1950–1999
                1 + random.nextInt(12),   // month
                1 + random.nextInt(28)    // day (safe range)
        ));
        customer.setIsPremium(random.nextBoolean() ? 'Y' : 'N');
        System.out.println(customer);
        return customer;
    }
}