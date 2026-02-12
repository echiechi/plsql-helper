package com.plsql.tools.handlers;

import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.TemplateManager;
import com.plsql.tools.templates.CodeSnippetsTemplatesManager;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.Map;

import static com.plsql.tools.templates.CodeSnippetsTemplatesManager.PROCESS_SIMPLE_RESULT_SET;
import static com.plsql.tools.tools.CodeGenConstants.variableName;

public class SimpleReturnHandler implements ReturnTypeHandler {
    private final TemplateManager<CodeSnippets.SimpleResultSetParams> templateManager = new CodeSnippetsTemplatesManager<>();

    @Override
    public boolean canHandle(ReturnElementInfo returnElement) {
        return returnElement.getTypeInfo().isSimple();
    }

    @Override
    public String generateCode(ReturnElementInfo returnElement) {
        var context = Map.of(
                CodeSnippets.SimpleResultSetParams.POSITION, returnElement.getPos(),
                CodeSnippets.SimpleResultSetParams.STMT_RESULT_TYPE, returnElement.getTypeInfo().typeAsString(),
                CodeSnippets.SimpleResultSetParams.STMT_GETTER, returnElement.getTypeInfo().asTypeMapper().getJdbcGetterMethod(),
                CodeSnippets.SimpleResultSetParams.OBJECT_INIT_STATEMENT, variableName(returnElement.getName()),
                CodeSnippets.SimpleResultSetParams.STMT_VAR_NAME, CodeGenConstants.STATEMENT_VAR);
        return templateManager
                .render(PROCESS_SIMPLE_RESULT_SET, context);
    }
}
