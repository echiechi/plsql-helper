package com.plsql.tools.example;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;

@Package(name = "pkg_customer_management")
public abstract class Example extends DataSourceAware {

    public Example(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Procedure(name = "get_customers_by_criteria", dataSource = DataSources.MY_DS)
    public abstract Object getCustomersByCriteria(Customer customer);

    @Procedure(name = "display_message_customer", dataSource = DataSources.MY_DS)
    public abstract void voidCustomerTest();

    @Procedure(name = "display_message_customer", dataSource = DataSources.MY_DS)
    public abstract void params2CustomersTest(Customer customer1, int test, String test2);

}
