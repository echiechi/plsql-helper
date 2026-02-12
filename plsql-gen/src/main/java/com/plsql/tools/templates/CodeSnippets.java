package com.plsql.tools.templates;

public class CodeSnippets {
    public enum ResultSetParams {
        STMT_RESULT_TYPE,
        STMT_RESULT_VAR,
        STMT_VAR_NAME,
        STMT_GETTER,
        POSITION,
        SETTER_STATEMENTS,
        HANDLE_EMPTY_STATEMENT;
    }

    public enum SimpleResultSetParams {
        STMT_RESULT_TYPE,
        OBJECT_INIT_STATEMENT,
        STMT_VAR_NAME,
        STMT_GETTER,
        POSITION;
    }

    public enum OptionalResultSetParams {
        STMT_RESULT_TYPE,
        OBJECT_INIT_STATEMENT,
        OPTIONAL_TYPE,
        STMT_VAR_NAME,
        STMT_GETTER,
        POSITION;
    }

    public enum MethodParams {
        RETURN_TYPE, METHOD_NAME, PARAMETERS, DATA_SOURCE, TRANSACTIONAL_METHOD;
    }

    public enum CallableMethodParams {
        STATEMENT_STATIC_CALL, RETURN_TYPE, METHOD_NAME, PARAMETERS, PROCEDURE_FULL_NAME, INIT_POS, STATEMENT_POPULATION,
        REGISTER_OUT_PARAM, RESULT_SET_EXTRACTION, RETURN_STATEMENT;
    }
}
