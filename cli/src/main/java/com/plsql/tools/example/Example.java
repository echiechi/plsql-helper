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
    public abstract void getCustomersByCriteria(Customer customer);
}
