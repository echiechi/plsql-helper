package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Function;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.processors.MethodProcessingResult;
import com.plsql.tools.processors.MethodProcessor;
import com.plsql.tools.statement.ParameterType;
import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.templates.Templates;
import com.plsql.tools.tools.ElementTools;
import com.plsql.tools.utils.CaseConverter;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.ST;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.Tools.*;

public class FunctionMethodGenerator { // TODO : can add interface for procedure/function call
    private final ProcessingContext context;
    private final TypeElement packageClass;
    private final ExecutableElement method;
    private final MethodProcessor methodProcessor;
    private final ElementTools elementTools;

    public FunctionMethodGenerator(ProcessingContext context, TypeElement packageClass, ExecutableElement method) {
        this.context = context;
        this.packageClass = packageClass;
        this.method = method;
        this.methodProcessor = new MethodProcessor(context);
        this.elementTools = new ElementTools(context);
    }

    public String generate() {
        MethodProcessingResult result = methodProcessor.process(method);
        Package packageAnnotation = packageClass.getAnnotation(Package.class);
        Function functionAnnotation = method.getAnnotation(Function.class);

        var packageName = StringUtils.isBlank(packageAnnotation.name()) ?
                CaseConverter.toSnakeCase(packageClass.getSimpleName().toString()) :
                packageAnnotation.name();
        var procedureName = StringUtils.isBlank(functionAnnotation.name()) ?
                CaseConverter.toSnakeCase(method.getSimpleName().toString()) :
                functionAnnotation.name();

        context.logInfo("Extract procedure outputs", result.getReturnResult()); // TODO : check output can't be void
        var outputs = elementTools.extractOutputs(result.getReturnResult()); // TODO: check output in function should be only one no multiple Outputs
        context.logDebug("Outputs:", outputs.stream().map(String::valueOf).collect(Collectors.joining("|")));

        context.logInfo("Generate JDBC call");
        var functionCallGenerator = new FunctionCallGenerator(
                packageName,
                procedureName);
        result.getParameterNames().forEach(functionCallGenerator::withParameter);
        // outputs.forEach(o -> functionCallGenerator.withParameter(o.output.value()));
        var functionCall = functionCallGenerator.build();
        context.logDebug("Procedure call:", functionCall);

        context.logInfo("Generate Statement population from class parameters");
        var stmtPopulationBuilder = new StmtPopulationGenerator(context, true);
        var populationStatements = String.join("\n", stmtPopulationBuilder.generateStatements(result.getParameterInfo()));
        context.logDebug("Statement population:", populationStatements);

        context.logInfo("Generate output registration");
        String registeredOutParameters;
        if (result.getReturnResult() == null) {
            registeredOutParameters = "";
        } else {
            var outRegistrationGenerator = new OutRegistrationGenerator(context);
            registeredOutParameters = String.join("\n", outRegistrationGenerator
                    .generateOutStatements(outputs));
        }
        context.logDebug("Registering out statements:", registeredOutParameters);

        context.logInfo("Generate ResultSet mapping...");
        String extractedOutputs;
        if (result.getReturnResult() == null) {
            extractedOutputs = "";
        } else {
            // set first position :
            outputs.getFirst().pos = "1";
            var outputGeneration = new OutputPopulationGenerator(context, outputs);
            extractedOutputs = String.join("\n",
                    outputGeneration.generateStatements(result.getReturnResult()));
        }
        context.logDebug("Extracting outputs from statement:", extractedOutputs);

        context.logInfo("Build method template...");
        return buildMethodTemplate(functionCall,
                method.getReturnType().toString(),
                method.getSimpleName().toString(),
                method.getParameters().stream().map(v -> String.format("%s %s", v.asType(), v.getSimpleName()))
                        .collect(Collectors.joining(", ")),
                functionAnnotation.dataSource(),
                String.format("%s_%s", packageName, procedureName),
                populationStatements,
                registeredOutParameters,
                extractedOutputs
        );
    }

    private String buildMethodTemplate(
            String procedureCall,
            String returnType,
            String methodName,
            String parameters,
            String dataSource,
            String procedureName,
            String populationStatements,
            String registeredOutParameters,
            String resultSetsExtractionStatements) {
        ST templateBuilder = new ST(Templates.FUNCTION_METHOD_TEMPLATE);
        templateBuilder.add(TemplateParams.STATEMENT_STATIC_CALL.name(), procedureCall);

        templateBuilder.add(TemplateParams.RETURN_TYPE.name(), returnType);

        templateBuilder.add(TemplateParams.METHOD_NAME.name(), methodName);
        templateBuilder.add(TemplateParams.PARAMETERS.name(), parameters);
        templateBuilder.add(TemplateParams.DATA_SOURCE.name(), dataSource);
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(), procedureName);
        templateBuilder.add(TemplateParams.INIT_POS.name(), !parameters.isEmpty() ? "int pos = 1;" : "");
        templateBuilder.add(TemplateParams.STATEMENT_POPULATION.name(), populationStatements);
        templateBuilder.add(TemplateParams.REGISTER_OUT_PARAM.name(),
                isVoid(returnType) ? "" : registeredOutParameters);
        templateBuilder.add(TemplateParams.RESULT_SET_EXTRACTION.name(), isVoid(returnType) ? "" : resultSetsExtractionStatements);

        templateBuilder.add(TemplateParams.RETURN_STATEMENT.name(),
                isNullOrEmpty(returnType) || isVoid(returnType) ? "" : String.format("return %s__$;", RETURN_NAME));

        return templateBuilder.render();
    }
}
