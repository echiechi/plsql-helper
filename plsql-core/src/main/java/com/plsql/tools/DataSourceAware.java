package com.plsql.tools;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DataSourceAware {
    protected DataSourceProvider dataSourceProvider;
    public DataSourceAware(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    protected Connection openCnx(String ds) throws SQLException {
        return dataSourceProvider.getDataSource(ds).getConnection();
    }

    protected void closeCnx(Connection cnx) throws SQLException {
        cnx.close();
    }
}
