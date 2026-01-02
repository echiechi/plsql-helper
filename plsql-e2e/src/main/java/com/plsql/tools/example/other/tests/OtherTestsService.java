package com.plsql.tools.example.other.tests;

import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.example.DataSources;

@Package(name = "pkg_test_management")
public abstract class OtherTestsService extends DataSourceAware {
    public OtherTestsService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @PlsqlCallable(name = "display_message_Test", dataSource = DataSources.MY_DS)
    public abstract void displayMessageTest(ObjectTest objectTest);


    @PlsqlCallable(name = "display_message_Test2", dataSource = DataSources.MY_DS, outputs = {})
    public abstract void/*ObjectTest*/ getMessageTest(ObjectTest objectTest);
}
