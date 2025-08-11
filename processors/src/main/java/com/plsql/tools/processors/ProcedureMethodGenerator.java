package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Param;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.statement.ParameterType;
import com.plsql.tools.statement.ProcedureExecutionStatement;
import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.templates.Templates;
import com.plsql.tools.tools.statement.StatementGenerator;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static com.plsql.tools.tools.Tools.isNullOrEmpty;
import static com.plsql.tools.tools.Tools.isVoid;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

public class ProcedureMethodGenerator {
    private final ProcessingContext context;
    private final ExecutableElement method;
    private final Package packageAnnotation;
    private final Procedure methodAnnotation;
    private final StatementGenerator generator;
    private final MethodProcessor methodProcessor;

    public ProcedureMethodGenerator(ProcessingContext context, RoundEnvironment roundEnv,
                                    ExecutableElement method, Package packageAnnotation,
                                    Procedure methodAnnotation) {
        this.context = context;
        this.method = method;
        this.packageAnnotation = packageAnnotation;
        this.methodAnnotation = methodAnnotation;
        this.generator = new StatementGenerator(context);
        this.methodProcessor = new MethodProcessor(context, roundEnv);
    }

    public String generate() {
        String returnType = method.getReturnType().toString();
        String methodName = method.getSimpleName().toString();

        context.logInfo("Generating method: " + methodName);
        MethodProcessingResult result = methodProcessor.process(method);

        if (!result.isValid()) {
            throw new IllegalStateException("Parameter processing failed for method: " + methodName);
        }

        String statement = createProcedureStatement(methodName, result, returnType);
        String setStatements = String.join("\n",
                generator.generateSetStatements(result.getMethodParameters()).getStatements());

        String outStatements = String.join("\n",
                generator.generateOutStatements(methodAnnotation.outputs()).getStatements()
        );

        // TODO : remove ?
        String getterStatements = String.join("\n",
                generator.generateGetStatements(methodAnnotation.outputs()).getStatements()
        );

        String resultSetsExtractionStatements = String.join("\n",
                generator.generateResultSetExtraction(methodAnnotation.outputs(), result.getMethodReturns())
                        .getStatements()
        );
        return buildMethodTemplate(
                returnType,
                methodName,
                result.getParameters(),
                statement,
                setStatements,
                outStatements,
                getterStatements,
                resultSetsExtractionStatements
        );
    }

    private String createProcedureStatement(String methodName,
                                            MethodProcessingResult result,
                                            String returnType) {
        String procedureName = CaseConverter.toSnakeCase(methodName).toUpperCase();
        String procedureCallName = methodAnnotation.name();

        var statement = new ProcedureExecutionStatement(
                packageAnnotation.name(), procedureName, procedureCallName);

        result.getMethodParameters().forEach((field) -> {
            Param param = field.getParameter().getAnnotation(Param.class);
            String paramName = (param != null && isNoneBlank(param.value()))
                    ? param.value()
                    : CaseConverter.toSnakeCase(field.getParameter().getSimpleName().toString());
            statement.withParameter(paramName);
        });
        if (!isVoid(returnType)) {
            for (Output o : methodAnnotation.outputs()) {
                statement.withParameter(o.name(), ParameterType.OUT);
            }
        }
        return statement.build();
    }

    private String buildMethodTemplate(String returnType, String methodName, List<String> parameters,
                                       String statement,
                                       String setStatements,
                                       String outStatements,
                                       String getObjectStatements,
                                       String resultSetsExtractionStatements) {
        ST templateBuilder = new ST(Templates.METHOD_TEMPLATE);
        templateBuilder.add(TemplateParams.STATEMENT_STATIC_CALL.name(), statement);
        templateBuilder.add(TemplateParams.RETURN_TYPE.name(), returnType);
        templateBuilder.add(TemplateParams.METHOD_NAME.name(), methodName);
        templateBuilder.add(TemplateParams.PARAMETERS.name(), String.join(", ", parameters));
        templateBuilder.add(TemplateParams.DATA_SOURCE.name(), methodAnnotation.dataSource());
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(), CaseConverter.toSnakeCase(methodName).toUpperCase());
        templateBuilder.add(TemplateParams.INIT_POS.name(), !parameters.isEmpty() ? "int pos = 1;" : "");
        templateBuilder.add(TemplateParams.STATEMENT_POPULATION.name(), setStatements);
        templateBuilder.add(TemplateParams.REGISTER_OUT_PARAM.name(),
                isVoid(returnType) ? "" : outStatements);
        // templateBuilder.add(TemplateParams.OUT_STATEMENTS.name(), isVoid(returnType) ? "" : getObjectStatements);

        templateBuilder.add(TemplateParams.RESULT_SET_EXTRACTION.name(), isVoid(returnType) ? "" : resultSetsExtractionStatements);

        templateBuilder.add(TemplateParams.RETURN_STATEMENT.name(), isNullOrEmpty(returnType) || isVoid(returnType) ? "" : "return toReturn;");

        return templateBuilder.render();
    }
}
