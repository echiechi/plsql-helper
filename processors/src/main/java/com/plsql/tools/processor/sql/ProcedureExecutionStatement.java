package com.plsql.tools.processor.sql;

import com.plsql.tools.processor.TemplateParams;
import com.plsql.tools.utils.PlsqlNameValidator;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class ProcedureExecutionStatement {
    public static final String PROCEDURE_CALL_TEMPLATE = """
            public static final String <procedureFullName> = "{ call <procedureCallName>(<procedureParameters>) }";
            """;
    private final String procedureName;
    private final List<String> params;

    public ProcedureExecutionStatement(String procedureName) {
        if (!PlsqlNameValidator.isValidPlsqlName(procedureName)) {
            throw new IllegalArgumentException(String.format("PROCEDURE NAME IS INVALID %s", procedureName));
        }
        this.procedureName = procedureName;
        this.params = new ArrayList<>();
    }

    public void addParameter(String param) {
        if (!PlsqlNameValidator.isValidPlsqlName(param)) {
            throw new IllegalArgumentException(String.format("PARAMETER NAME IS INVALID %s", param));
        }
        params.add(String.format("%s => ?", param));
    }

    public String build() {
        ST templateBuilder = new ST(PROCEDURE_CALL_TEMPLATE);
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.getValue(), procedureName.replace(".", "_"));
        templateBuilder.add(TemplateParams.PROCEDURE_CALL_NAME.getValue(), procedureName);
        templateBuilder.add(TemplateParams.PROCEDURE_PARAMETERS.getValue(), String.join(",", params));
        return templateBuilder.render();
    }

}