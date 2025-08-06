package com.plsql.tools.sql;

import java.util.Map;

public class StatementBuildResult {
    private final String statement;
    private final Map<String, Object> buildInfo;
    private final String errorMessage;
    private final boolean success;

    private StatementBuildResult(String statement, Map<String, Object> buildInfo, String errorMessage, boolean success) {
        this.statement = statement;
        this.buildInfo = buildInfo;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public static StatementBuildResult success(String statement, Map<String, Object> buildInfo) {
        return new StatementBuildResult(statement, buildInfo, null, true);
    }

    public static StatementBuildResult failure(String errorMessage) {
        return new StatementBuildResult(null, null, errorMessage, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatement() {
        return statement;
    }

    public Map<String, Object> getBuildInfo() {
        return buildInfo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
