package com.plsql.tools.example.customer;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.example.DataSources;

import java.util.List;

@Package(name = "pkg_customer_management")
public abstract class CustomerService2 extends DataSourceAware {
    public CustomerService2(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @PlsqlCallable(name = "insert_customer", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_id"))
    public abstract Integer insertCustomerObject(ComposedCustomerInsert customerInsert);


    @PlsqlCallable(name = "insert_customer", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_return"))
    public abstract Customer insertCustomerObject(Customer customer);

    @PlsqlCallable(name = "insert_customer", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_return"))
    public abstract List<Customer> insertCustomerObject(int test);

    @PlsqlCallable(name = "get_all_customers", dataSource = DataSources.MY_DS, outputs = @Output("p_customer_return"))
    public abstract List<CustomerGet> getAllCustomerObject(int test);

}
