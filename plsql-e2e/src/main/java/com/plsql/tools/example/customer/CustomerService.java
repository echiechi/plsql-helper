package com.plsql.tools.example.customer;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.annotations.PlsqlParam;
import com.plsql.tools.enums.CallableType;
import com.plsql.tools.example.DataSources;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Package(name = "pkg_customer_management")
public abstract class CustomerService extends DataSourceAware {
    public CustomerService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @PlsqlCallable(name = "insert_customer", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_id"))
    public abstract Integer insertCustomerObject(@PlsqlParam("p_first_name")
                                                 String firstName,
                                                 @PlsqlParam("p_last_name")
                                                 String lastName,
                                                 @PlsqlParam("p_email")
                                                 String email,
                                                 @PlsqlParam("p_phone")
                                                 String phone,                // default: null
                                                 @PlsqlParam("p_age")
                                                 Integer age,                 // default: null
                                                 @PlsqlParam("p_credit_limit")
                                                 Double creditLimit,
                                                 @PlsqlParam("p_account_balance")
                                                 Double accountBalance,
                                                 @PlsqlParam("p_address_line1")
                                                 String addressLine1,         // default: null
                                                 @PlsqlParam("p_address_line2")
                                                 String addressLine2,         // default: null
                                                 @PlsqlParam("p_city")
                                                 String city,                 // default: null
                                                 @PlsqlParam("p_state_province")
                                                 String stateProvince,        // default: null
                                                 @PlsqlParam("p_postal_code")
                                                 String postalCode,           // default: null
                                                 @PlsqlParam("p_country")
                                                 String country,
                                                 @PlsqlParam("p_date_of_birth")
                                                 LocalDate dateOfBirth,       // default: null
                                                 @PlsqlParam("p_is_premium")
                                                 char isPremium);

    @PlsqlCallable(name = "display_message_customer", dataSource = DataSources.MY_DS)
    public abstract void displayMessageCustomer();


    @PlsqlCallable(name = "get_customer_by_id", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_data"))
    public abstract Optional<CustomerGet> getCustomerById(@PlsqlParam("p_customer_id") long id);

    @PlsqlCallable(name = "get_customer_by_id2", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_data"))
    public abstract List<CustomerGet> getAllCustomers2(); // TODO: handle better same method name/ same proc stock name in different methods

    @PlsqlCallable(name = "get_customers_by_criteria", dataSource = DataSources.MY_DS, outputs = @Output(value = "p_customer_cursor"))
    public abstract CustomerGet getCustomerByCrit(@PlsqlParam("p_last_name")
                                                  String lastName,
                                                  @PlsqlParam("p_email")
                                                  String email,
                                                  @PlsqlParam("p_city")
                                                  String city,
                                                  @PlsqlParam("p_is_active")
                                                  char isActive);

    @PlsqlCallable(name = "get_all_customers", dataSource = DataSources.MY_DS, outputs = {
            @Output(value = "p_customer_cursor", field = "customerGets"),
            @Output(value = "p_total_count", field = "customerTotal")})
    public abstract CustomerMulti getAllCustomers(@PlsqlParam("p_page_size")
                                                  int pageSize,
                                                  @PlsqlParam("p_page_number")
                                                  int pageNumber);

    @PlsqlCallable(name = "update_customer", dataSource = DataSources.MY_DS)
    public abstract void updateCustomer(
            @PlsqlParam("p_customer_id") long id,
            @PlsqlParam("p_first_name") String firstName,
            @PlsqlParam("p_last_name") String lastName,
            @PlsqlParam("p_email") String email,
            @PlsqlParam("p_phone") String phone,
            @PlsqlParam("p_is_active") Character isActive,
            @PlsqlParam("p_is_premium") char isPremium,
            @PlsqlParam("p_last_login") LocalDateTime lastLogin
    );

    @PlsqlCallable(name = "get_customer_full_name",
            outputs = {@Output("customer_full_name")},
            dataSource = DataSources.MY_DS,
            type = CallableType.FUNCTION)
    public abstract String getCustomerFullName(@PlsqlParam("p_customer_id") long id);

    public abstract String getCustomerFullName(Connection cnx, long id);

}
