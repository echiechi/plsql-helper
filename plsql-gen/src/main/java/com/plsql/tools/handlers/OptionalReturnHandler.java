package com.plsql.tools.handlers;

import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.TemplateManager;
import com.plsql.tools.templates.CodeSnippetsTemplatesManager;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.plsql.tools.templates.CodeSnippetsTemplatesManager.PROCESS_OPTIONAL_RESULT_SET;
import static com.plsql.tools.tools.CodeGenConstants.variableName;
import static com.plsql.tools.tools.CodeGenConstants.wrappedVariableName;

public class OptionalReturnHandler implements ReturnTypeHandler {

    private final Extractor extractor;

    private final TemplateManager<CodeSnippets.OptionalResultSetParams> templateManager = new CodeSnippetsTemplatesManager<>();

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
            return templateManager
                    .render(PROCESS_OPTIONAL_RESULT_SET, context);
        } else {
            var defaultReturnName = returnElement.getName();
            var wrappedVariableName = wrappedVariableName(defaultReturnName);
            var optionalAssign = GenTools.optionalAssign(
                    variableName(defaultReturnName),
                    variableName(wrappedVariableName));
            ComposedReturnHandler composedReturnHandler = ComposedReturnHandler
                    .builder()
                    .extractor(extractor)
                    .isToAssign(false)
                    .isWrapped(true)
                    .isInitObject(false)
                    .isReturnSomething(true)
                    .toAppendToStatements(optionalAssign)
                    .build();
            var optionalInit = GenTools.optionalInit(
                    returnElement.getTypeInfo().wrappedTypeAsString(),
                    variableName(defaultReturnName));
            return optionalInit + "\n" + composedReturnHandler.generateCode(returnElement);
        }
    }
}
