package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.processors.MethodProcessingResult;
import com.plsql.tools.processors.MethodProcessor;
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

public class ProcedureMethodGenerator {
    private final ProcessingContext context;
    private final TypeElement packageClass;
    private final ExecutableElement method;
    private final MethodProcessor methodProcessor;
    private final ElementTools elementTools;

    public ProcedureMethodGenerator(ProcessingContext context,
                                    TypeElement packageClass,
                                    ExecutableElement method) {
        this.context = context;
        this.method = method;
        this.packageClass = packageClass;
        this.methodProcessor = new MethodProcessor(context);
        this.elementTools = new ElementTools(context);
    }

    // TODO : working example
    public String generate() {
        MethodProcessingResult result = methodProcessor.process(method);
        Package packageAnnotation = packageClass.getAnnotation(Package.class);
        Procedure procedureAnnotation = method.getAnnotation(Procedure.class);

        var packageName = StringUtils.isBlank(packageAnnotation.name()) ?
                CaseConverter.toSnakeCase(packageClass.getSimpleName().toString()) :
                packageAnnotation.name();
        var procedureName = StringUtils.isBlank(procedureAnnotation.name()) ?
                CaseConverter.toSnakeCase(method.getSimpleName().toString()) :
                procedureAnnotation.name();

        context.logInfo("Extract procedure outputs", result.getReturnResult());
        var outputs = elementTools.extractOutputs(result.getReturnResult());
        context.logDebug("Outputs:", outputs.stream().map(String::valueOf).collect(Collectors.joining("|")));

        context.logInfo("Generate JDBC call");
        var procedureCall = new CallStatementBuilder(
                packageName,
                procedureName);
        result.getParameterNames().forEach(procedureCall::withParameter);
        outputs.forEach(o -> procedureCall.withParameter(o.output.value()));
        var procedureCallStatement = procedureCall.build();
        context.logDebug("Procedure call:", procedureCallStatement);

        context.logInfo("Generate Statement population from class parameters");
        var stmtPopulationBuilder = new StmtPopulationGenerator(context);
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
            var outputGeneration = new OutputPopulationGenerator(context);
            extractedOutputs = String.join("\n",
                    outputGeneration.generateStatements(result.getReturnResult()));
        }
        context.logDebug("Extracting outputs from statement:", extractedOutputs);

        context.logInfo("Build method template...");
        return buildMethodTemplate(procedureCallStatement,
                method.getReturnType().toString(),
                method.getSimpleName().toString(),
                method.getParameters().stream().map(v -> String.format("%s %s", v.asType(), v.getSimpleName()))
                        .collect(Collectors.joining(", ")),
                procedureAnnotation.dataSource(),
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
        ST templateBuilder = new ST(Templates.METHOD_TEMPLATE);
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
