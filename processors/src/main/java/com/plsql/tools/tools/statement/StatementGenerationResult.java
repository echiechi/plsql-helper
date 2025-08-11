package com.plsql.tools.tools.statement;

import java.util.ArrayList;
import java.util.List;

// TODO : remove results object and replace with exceptions here and there
public class StatementGenerationResult {
    private final List<String> statements;
    private final List<String> warnings;
    private final List<String> errors;

    private final boolean success;

    private StatementGenerationResult(List<String> statements, List<String> warnings,
                                      List<String> errors, boolean success) {
        this.statements = statements != null ? statements : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
        this.success = success;
    }

    public static StatementGenerationResult success(List<String> statements) {
        return new StatementGenerationResult(statements, null, null, true);
    }

    public static StatementGenerationResult success(List<String> statements, List<String> warnings) {
        return new StatementGenerationResult(statements, warnings, null, true);
    }

    public static StatementGenerationResult failure(List<String> errors) {
        return new StatementGenerationResult(null, null, errors, false);
    }

    public List<String> getStatements() {
        return statements;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return success;
    }

}
