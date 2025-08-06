package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.TemplateParams;
import com.plsql.tools.Templates;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Param;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.processors.params.MethodParameterProcessor;
import com.plsql.tools.processors.params.ProcessingResult;
import com.plsql.tools.sql.ProcedureExecutionStatement;
import com.plsql.tools.tools.statement.StatementGenerator;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static com.plsql.tools.tools.Tools.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

public class ProcedureMethodGenerator {
    private final ProcessingContext context;
    private final RoundEnvironment roundEnv;
    private final ExecutableElement method;
    private final Package packageAnnotation;
    private final Procedure methodAnnotation;
    private final StatementGenerator generator;
    private final MethodParameterProcessor paramProcessor;

    public ProcedureMethodGenerator(ProcessingContext context, RoundEnvironment roundEnv,
                                    ExecutableElement method, Package packageAnnotation,
                                    Procedure methodAnnotation) {
        this.context = context;
        this.roundEnv = roundEnv;
        this.method = method;
        this.packageAnnotation = packageAnnotation;
        this.methodAnnotation = methodAnnotation;
        this.generator = new StatementGenerator(context);
        this.paramProcessor = new MethodParameterProcessor(context, roundEnv);

    }

    public String generate() {
        String returnType = method.getReturnType().toString();
        String methodName = method.getSimpleName().toString();

        context.logInfo("Generating method: " + methodName);
        ProcessingResult result = paramProcessor.processParameters(method);

        if (!result.isValid()) {
            throw new IllegalStateException("Parameter processing failed for method: " + methodName);
        }

        ProcedureExecutionStatement statement = createProcedureStatement(methodName, result);
        String setStatements = String.join("\n",
                generator.generateSetStatements(result.getExtractedMap()).getStatements());

        return buildMethodTemplate(returnType, methodName, result.getParameters(), statement, setStatements);
    }

    private ProcedureExecutionStatement createProcedureStatement(String methodName,
                                                                 ProcessingResult result) {
        String procedureName = CaseConverter.toSnakeCase(methodName).toUpperCase();
        String procedureCallName = methodAnnotation.name();

        var statement = ProcedureExecutionStatement.builder(
                packageAnnotation.name(), procedureName, procedureCallName);

        result.getExtractedMap().forEach((field, element) -> {
            Param param = field.getField().getAnnotation(Param.class);
            String paramName = (param != null && isNoneBlank(param.value()))
                    ? param.value()
                    : CaseConverter.toSnakeCase(field.getField().getSimpleName().toString());
            statement.withParameter(paramName);
        });

        return statement.build();
    }

    private String buildMethodTemplate(String returnType, String methodName, List<String> parameters,
                                       ProcedureExecutionStatement statement, String setStatements) {
        ST templateBuilder = new ST(Templates.METHOD_TEMPLATE);
        templateBuilder.add(TemplateParams.STATEMENT_STATIC_CALL.name(), statement.build());
        templateBuilder.add(TemplateParams.RETURN_TYPE.name(), returnType);
        templateBuilder.add(TemplateParams.RETURN_STATEMENT.name(), isNullOrEmpty(returnType) || "void".equals(returnType) ? "" : "return null;");
        templateBuilder.add(TemplateParams.METHOD_NAME.name(), methodName);
        templateBuilder.add(TemplateParams.PARAMETERS.name(), String.join(",", parameters));
        templateBuilder.add(TemplateParams.DATA_SOURCE.name(), methodAnnotation.dataSource());
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(), CaseConverter.toSnakeCase(methodName).toUpperCase());
        templateBuilder.add(TemplateParams.INIT_POS.name(), !parameters.isEmpty() ? "int pos = 1;" : "");
        templateBuilder.add(TemplateParams.STATEMENT_POPULATION.name(), setStatements);
        templateBuilder.add(TemplateParams.REGISTER_OUT_PARAM.name(),
                "void".equals(returnType) ? "" : "stmt.registerOutParameter(pos, OracleTypes.CURSOR);");

        return templateBuilder.render();
    }
}
