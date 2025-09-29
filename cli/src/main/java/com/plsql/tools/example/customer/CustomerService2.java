package com.plsql.tools.example.customer;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.example.DataSources;

@Package(name = "pkg_customer_management")
public abstract class CustomerService2 extends DataSourceAware {
    public CustomerService2(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Procedure(name = "insert_customer", dataSource = DataSources.MY_DS)
    public abstract @Output("p_customer_id") Integer insertCustomerObject(CustomerInsert customerInsert);

}
