package com.plsql.tools.statement.generators;

import com.plsql.tools.statement.CallGenerator;
import org.stringtemplate.v4.ST;

import static com.plsql.tools.templates.FunctionCallParams.*;

public class FunctionCallGenerator extends CallGenerator { // TODO : can add interface for procedure/function call
    public static final String FUNCTION_CALL_TEMPLATE = """
            public static final String <FUNCTION_FULL_NAME><SUFFIX> = "{ ? = call <PACKAGE_CALL_NAME><FUNCTION_CALL_NAME>(<FUNCTION_PARAMETERS>) }";
            """;

    public FunctionCallGenerator(String packageName, String name) {
        super(packageName, name);
    }

    /**
     * Builds the statement using StringTemplate
     */
    @Override
    public String buildWithTemplate() {
        ST templateBuilder = new ST(FUNCTION_CALL_TEMPLATE);
        templateBuilder.add(FUNCTION_FULL_NAME.name(),
                formatFullName());
        templateBuilder.add(SUFFIX.name(), suffix);
        templateBuilder.add(FUNCTION_CALL_NAME.name(), name);
        templateBuilder.add(FUNCTION_PARAMETERS.name(),
                formatParameters());
        templateBuilder.add(PACKAGE_CALL_NAME.name(),
                formatCallName());
        return templateBuilder.render();
    }
}
