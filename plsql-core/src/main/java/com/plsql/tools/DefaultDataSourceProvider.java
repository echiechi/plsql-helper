package com.plsql.tools;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultDataSourceProvider implements DataSourceProvider {
    private final Map<String, DataSource> map = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void registerDataSource(String dsName, DataSource ds) {
        map.put(dsName, ds);
    }

    @Override
    public DataSource getDataSource(String dsName) {
        return map.get(dsName);
    }
}
