package com.plsql.tools.example.customer;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Input;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.example.DataSources;

import java.time.LocalDate;

@Package(name = "pkg_customer_management")
public abstract class CustomerService extends DataSourceAware {
    public CustomerService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Procedure(name = "insert_customer", dataSource = DataSources.MY_DS)
    public abstract @Output("p_customer_id") Integer insertCustomerObject(@Input("p_first_name")
                                                                          String firstName,
                                                                          @Input("p_last_name")
                                                                          String lastName,
                                                                          @Input("p_email")
                                                                          String email,
                                                                          @Input("p_phone")
                                                                          String phone,                // default: null
                                                                          @Input("p_age")
                                                                          Integer age,                 // default: null
                                                                          @Input("p_credit_limit")
                                                                          Double creditLimit,
                                                                          @Input("p_account_balance")
                                                                          Double accountBalance,
                                                                          @Input("p_address_line1")
                                                                          String addressLine1,         // default: null
                                                                          @Input("p_address_line2")
                                                                          String addressLine2,         // default: null
                                                                          @Input("p_city")
                                                                          String city,                 // default: null
                                                                          @Input("p_state_province")
                                                                          String stateProvince,        // default: null
                                                                          @Input("p_postal_code")
                                                                          String postalCode,           // default: null
                                                                          @Input("p_country")
                                                                          String country,
                                                                          @Input("p_date_of_birth")
                                                                          LocalDate dateOfBirth,       // default: null
                                                                          @Input("p_is_premium")
                                                                          char isPremium);

    @Procedure(name = "display_message_customer", dataSource = DataSources.MY_DS)
    public abstract void displayMessageCustomer();


    @Procedure(name = "get_customer_by_id", dataSource = DataSources.MY_DS)
    public abstract CustomerGet getCustomerById(@Input("p_customer_id") long id);

    @Procedure(name = "get_customers_by_criteria", dataSource = DataSources.MY_DS)
    public abstract @Output("p_customer_cursor") CustomerGet getCustomerByCrit(@Input("p_last_name")
                                                                               String lastName,
                                                                               @Input("p_email")
                                                                               String email,
                                                                               @Input("p_city")
                                                                               String city,
                                                                               @Input("p_is_active")
                                                                               char isActive);

}
