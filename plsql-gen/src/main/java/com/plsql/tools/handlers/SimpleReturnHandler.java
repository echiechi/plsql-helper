package com.plsql.tools.handlers;

import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.ReturnCodeTemplateManager;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.Map;

import static com.plsql.tools.tools.CodeGenConstants.variableName;

public class SimpleReturnHandler implements ReturnTypeHandler {
    private final ReturnCodeTemplateManager returnCodeTemplateManager = new ReturnCodeTemplateManager();

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
        return returnCodeTemplateManager
                .renderSimpleProcessResultSet(context);
    }
}
