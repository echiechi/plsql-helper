package com.plsql.tools.handlers;

import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.ReturnCodeTemplateManager;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.plsql.tools.tools.CodeGenConstants.variableName;
import static com.plsql.tools.tools.CodeGenConstants.wrappedVariableName;

public class OptionalReturnHandler implements ReturnTypeHandler {

    private final Extractor extractor;

    private final ReturnCodeTemplateManager returnCodeTemplateManager = new ReturnCodeTemplateManager();

    public OptionalReturnHandler(Extractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public boolean canHandle(ReturnElementInfo returnElement) {
        return returnElement.getTypeInfo().isWrapped() &&
                extractor.isOptional(returnElement.getTypeInfo().getMirror());
    }

    @Override
    public String generateCode(ReturnElementInfo returnElement) {
        if (returnElement.getTypeInfo().isWrappedSimple()) {
            var typeInfo = returnElement.getTypeInfo();
            var context = Map.of(
                    CodeSnippets.OptionalResultSetParams.POSITION, returnElement.getPos(),
                    CodeSnippets.OptionalResultSetParams.STMT_RESULT_TYPE,
                    GenTools.genericType(Optional.class.getCanonicalName(), typeInfo.wrappedTypeAsString()),
                    CodeSnippets.OptionalResultSetParams.STMT_GETTER, Objects.requireNonNull(typeInfo.wrappedTypeAsTypeMapper()).getJdbcGetterMethod(),
                    CodeSnippets.OptionalResultSetParams.OBJECT_INIT_STATEMENT, variableName(returnElement.getName()),
                    CodeSnippets.OptionalResultSetParams.OPTIONAL_TYPE, Optional.class.getCanonicalName(),
                    CodeSnippets.OptionalResultSetParams.STMT_VAR_NAME, CodeGenConstants.STATEMENT_VAR);
            return returnCodeTemplateManager
                    .renderOptionalProcessResultSet(context);
        } else {
            var defaultReturnName = returnElement.getName();
            var wrappedVariableName = wrappedVariableName(defaultReturnName);
            var optionalAssign = GenTools.optionalAssign(
                    variableName(defaultReturnName),
                    variableName(wrappedVariableName));
            ComposedReturnHandler composedReturnHandler = new ComposedReturnHandler(
                    extractor,
                    false,
                    true,
                    false,
                    true,
                    optionalAssign);
            var optionalInit = GenTools.optionalInit(
                    returnElement.getTypeInfo().wrappedTypeAsString(),
                    variableName(defaultReturnName));
            return optionalInit + "\n" + composedReturnHandler.generateCode(returnElement);
        }
    }
}
