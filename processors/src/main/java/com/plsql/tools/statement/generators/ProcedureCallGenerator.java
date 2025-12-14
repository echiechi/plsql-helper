package com.plsql.tools.statement.generators;

import com.plsql.tools.statement.CallGenerator;
import com.plsql.tools.templates.TemplateParams;
import org.stringtemplate.v4.ST;

import static com.plsql.tools.templates.FunctionCallParams.SUFFIX;

public class ProcedureCallGenerator extends CallGenerator {
    public static final String PROCEDURE_CALL_TEMPLATE = """
            public static final String <PROCEDURE_FULL_NAME><SUFFIX> = "{ call <PACKAGE_CALL_NAME><PROCEDURE_CALL_NAME>(<PROCEDURE_PARAMETERS>) }";
            """;

    public ProcedureCallGenerator(String packageName, String name) {
        super(packageName, name);
    }

    /**
     * Builds the SQL call statement
     */
    public String build() {
        return buildWithTemplate();
    }

    /**
     * Builds the statement using StringTemplate
     */
    @Override
    public String buildWithTemplate() {
        ST templateBuilder = new ST(PROCEDURE_CALL_TEMPLATE);

        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(),
                formatFullName());
        templateBuilder.add(SUFFIX.name(), suffix);
        templateBuilder.add(TemplateParams.PROCEDURE_CALL_NAME.name(), name);
        templateBuilder.add(TemplateParams.PROCEDURE_PARAMETERS.name(),
                formatParameters());
        templateBuilder.add(TemplateParams.PACKAGE_CALL_NAME.name(),
                formatCallName());

        return templateBuilder.render();
    }
}

