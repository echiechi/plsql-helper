package com.plsql.tools;


import com.plsql.tools.example.Customer;
import com.plsql.tools.example.Example;
import com.plsql.tools.example.ExampleImpl;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.SQLException;
import java.util.Properties;

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
        Example exp = new ExampleImpl(dsProvider);
        var customer = new Customer("Smith", "john.smith@email.com", "New York", 'Y');
        var result = exp.getCustomersByCriteria(customer);
        System.out.println(result);
    }
}