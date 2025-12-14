package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.enums.CallableType;
import com.plsql.tools.processors.MethodToProcess;
import com.plsql.tools.statement.CallGenerator;
import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.utils.CaseConverter;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.ST;

import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.plsql.tools.templates.Templates.FUNCTION_METHOD_TEMPLATE;
import static com.plsql.tools.templates.Templates.PROCEDURE_METHOD_TEMPLATE;
import static com.plsql.tools.tools.Tools.*;

public class ProcedureMethodGenerator {
    private final ProcessingContext context;
    private final TypeElement packageClass;
    private final MethodToProcess methodToProcess;

    public ProcedureMethodGenerator(ProcessingContext context,
                                    TypeElement packageClass,
                                    MethodToProcess methodToProcess) {
        this.context = context;
        this.methodToProcess = methodToProcess;
        this.packageClass = packageClass;
    }

    // TODO : working example
    public String generate() {
        Package packageAnnotation = packageClass.getAnnotation(Package.class);
        PlsqlCallable plsqlCallableAnnotation = methodToProcess.method().getAnnotation(PlsqlCallable.class);
        List<ElementInfo> methodParameters = Extractor.getInstance().extractParams(methodToProcess.method());

        var packageName = StringUtils.isBlank(packageAnnotation.name()) ?
                CaseConverter.toSnakeCase(packageClass.getSimpleName().toString()) :
                packageAnnotation.name();
        var procedureName = StringUtils.isBlank(plsqlCallableAnnotation.name()) ?
                CaseConverter.toSnakeCase(methodToProcess.method().getSimpleName().toString()) :
                plsqlCallableAnnotation.name();

        var outputs = Arrays.asList(plsqlCallableAnnotation.outputs());
        context.logDebug("Outputs:", outputs.stream().map(String::valueOf).collect(Collectors.joining("|")));

        context.logInfoDeco("TYPE : ", plsqlCallableAnnotation.type());
        List<String> paramNames = Extractor.getInstance().extractPramNames(methodParameters);

        context.logInfo("Generate JDBC call");
        CallGenerator callable = null;
        if (plsqlCallableAnnotation.type() == CallableType.PROCEDURE) {
            callable = new ProcedureCallGenerator(
                    packageName,
                    procedureName);
            callable.withSuffix(methodToProcess.suffix());
            paramNames.forEach(callable::withParameter);
            for (var output : outputs) {
                callable.withParameter(output.value());
            }
        } else {
            callable = new FunctionCallGenerator(
                    packageName,
                    procedureName);
            callable.withSuffix(methodToProcess.suffix());
            paramNames.forEach(callable::withParameter);
            if (outputs.size() > 1) {
                throw new IllegalStateException("A Function type can have only one @Output and it must be a simple type");
            } else if (outputs.isEmpty()) {
                throw new IllegalStateException("A Function must have a return (@Output)");
            }
        }

        var procedureCallStatement = callable.generate();
        context.logDebug("PlsqlCallable call:", procedureCallStatement);

        context.logInfo("Generate Statement population from class parameters");
        var boundGeneratedStatements = new PlsqlParamBinderGenerator(methodParameters).generate();
        context.logDebug("Statement population:", boundGeneratedStatements);

        var extractedReturnInfo = Extractor.getInstance().extractReturn(methodToProcess.method());
        var outputRegistration = new OutputRegistrationGenerator(extractedReturnInfo).generate();
        context.logInfoDeco(outputRegistration);

        var returnGenerator = new ReturnGenerator(extractedReturnInfo);
        var generatedReturn = returnGenerator.generate();
        context.logInfoDeco(generatedReturn);

        context.logInfo("Build method template...");

        return buildMethodTemplate(
                plsqlCallableAnnotation.type() == CallableType.PROCEDURE ? PROCEDURE_METHOD_TEMPLATE : FUNCTION_METHOD_TEMPLATE,
                procedureCallStatement,
                methodToProcess.method().getReturnType().toString(),
                methodToProcess.method().getSimpleName().toString(),
                methodToProcess.method().getParameters().stream().map(v -> String.format("%s %s", v.asType(), v.getSimpleName()))
                        .collect(Collectors.joining(", ")),
                plsqlCallableAnnotation.dataSource(),
                String.format("%s_%s", packageName, procedureName),
                boundGeneratedStatements,
                outputRegistration,
                generatedReturn
        );
    }

    private String buildMethodTemplate(
            String template,
            String procedureCall,
            String returnType,
            String methodName,
            String parameters,
            String dataSource,
            String procedureName,
            String boundGeneratedStatements,
            String registeredOutParameters,
            String resultSetsExtractionStatements) {
        ST templateBuilder = new ST(template);
        templateBuilder.add(TemplateParams.STATEMENT_STATIC_CALL.name(), procedureCall);

        templateBuilder.add(TemplateParams.RETURN_TYPE.name(), returnType);

        templateBuilder.add(TemplateParams.METHOD_NAME.name(), methodName);
        templateBuilder.add(TemplateParams.PARAMETERS.name(), parameters);
        templateBuilder.add(TemplateParams.DATA_SOURCE.name(), dataSource);
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(), procedureName);
        templateBuilder.add(TemplateParams.INIT_POS.name(), !parameters.isEmpty() || !isVoid(returnType) ? "int pos = 1;" : "");
        templateBuilder.add(TemplateParams.STATEMENT_POPULATION.name(), boundGeneratedStatements);
        templateBuilder.add(TemplateParams.REGISTER_OUT_PARAM.name(),
                isVoid(returnType) ? "" : registeredOutParameters);
        templateBuilder.add(TemplateParams.RESULT_SET_EXTRACTION.name(), isVoid(returnType) ? "" : resultSetsExtractionStatements);

        templateBuilder.add(TemplateParams.RETURN_STATEMENT.name(),
                isNullOrEmpty(returnType) || isVoid(returnType) ? "" : String.format("return %s__$;", RETURN_NAME));

        return templateBuilder.render();
    }
}
