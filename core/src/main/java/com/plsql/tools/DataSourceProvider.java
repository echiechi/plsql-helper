package com.plsql.tools;

import javax.sql.DataSource;

public interface DataSourceProvider {
    void registerDataSource(String dsName, DataSource ds);

    DataSource getDataSource(String dsName);
}
