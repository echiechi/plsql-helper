package com.plsql.tools.statements.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.enums.CallableType;
import com.plsql.tools.processors.MethodToProcess;
import com.plsql.tools.statements.CallGenerator;
import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.CodeSnippetsTemplatesManager;
import com.plsql.tools.templates.TemplateManager;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.extractors.ExtractorValidator;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.MetaInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.utils.CaseConverter;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.plsql.tools.templates.CodeSnippetsTemplatesManager.*;
import static com.plsql.tools.tools.CodeGenConstants.*;
import static com.plsql.tools.tools.Tools.isNullOrEmpty;
import static com.plsql.tools.tools.Tools.isVoid;

public class CallableGenerator {
    private final ProcessingContext context;
    private final TypeElement packageClass;
    private final MethodToProcess methodToProcess;
    private final Extractor extractor;

    public CallableGenerator(ProcessingContext context,
                             TypeElement packageClass,
                             MethodToProcess methodToProcess) {
        this.context = context;
        this.methodToProcess = methodToProcess;
        this.packageClass = packageClass;
        this.extractor = new Extractor(context);
    }

    public String generate() {

        ExtractorValidator.validateCallableMethod(methodToProcess.method());

        Package packageAnnotation = packageClass.getAnnotation(Package.class);
        PlsqlCallable plsqlCallableAnnotation = methodToProcess.method().getAnnotation(PlsqlCallable.class);
        List<ElementInfo> methodParameters = extractor.extractParams(methodToProcess.method());

        // extraction of useful information
        var packageName = StringUtils.isBlank(packageAnnotation.name()) ?
                CaseConverter.toSnakeCase(packageClass.getSimpleName().toString()) :
                packageAnnotation.name();
        var procedureName = StringUtils.isBlank(plsqlCallableAnnotation.name()) ?
                CaseConverter.toSnakeCase(methodToProcess.method().getSimpleName().toString()) :
                plsqlCallableAnnotation.name();

        var outputs = Arrays.asList(Tools.extractMetaInfo(plsqlCallableAnnotation.outputs()));
        List<String> paramNames = extractor.extractPramNames(methodParameters);
        var extractedReturnInfo = extractor.extractReturn(methodToProcess.method());

        debugLog(outputs, paramNames, plsqlCallableAnnotation);

        // init generators :
        var paramBinderGenerator = new PlsqlParamBinderGenerator(methodParameters, plsqlCallableAnnotation.type() == CallableType.FUNCTION);
        var outputRegistrationGenerator = new OutputRegistrationGenerator(extractedReturnInfo);
        var returnGenerator = new ReturnGenerator(extractedReturnInfo, extractor);

        CallGenerator callGenerator = createCallGenerator(plsqlCallableAnnotation,
                packageName,
                procedureName,
                paramNames,
                outputs,
                extractedReturnInfo);

        context.logInfo("Build method template...");

        return buildMethod(plsqlCallableAnnotation,
                methodToProcess,
                callGenerator,
                paramBinderGenerator,
                outputRegistrationGenerator,
                returnGenerator
        );

    }

    private void debugLog(List<MetaInfo> outputs, List<String> paramNames, PlsqlCallable plsqlCallableAnnotation) {
        context.logDebug("Outputs:", outputs.stream().map(String::valueOf).collect(Collectors.joining("|")));
        context.logDebug("Parameters:", paramNames.stream().map(String::valueOf).collect(Collectors.joining("|")));
        context.logDebug("Callable Type: ", plsqlCallableAnnotation.type());
    }

    private CallGenerator createCallGenerator(PlsqlCallable plsqlCallableAnnotation,
                                              String packageName,
                                              String procedureName,
                                              List<String> paramNames,
                                              List<MetaInfo> outputs,
                                              List<ReturnElementInfo> extractedReturnInfo) {
        CallGenerator callGenerator;
        if (plsqlCallableAnnotation.type() == CallableType.PROCEDURE) {
            callGenerator = new ProcedureCallGenerator(
                    packageName,
                    procedureName);
            callGenerator.withSuffix(methodToProcess.suffix());
            paramNames.forEach(callGenerator::withParameter);
            for (var output : outputs) {
                callGenerator.withParameter(output.alias());
            }
        } else if (plsqlCallableAnnotation.type() == CallableType.FUNCTION) {
            extractedReturnInfo.get(0).setPos("1");
            callGenerator = new FunctionCallGenerator(
                    packageName,
                    procedureName);
            callGenerator.withSuffix(methodToProcess.suffix());
            paramNames.forEach(callGenerator::withParameter);
        } else {
            throw new IllegalStateException("Type not yet supported " + plsqlCallableAnnotation.type());
        }
        return callGenerator;
    }

    private String buildMethod(PlsqlCallable plsqlCallableAnnotation,
                               MethodToProcess methodToProcess,
                               CallGenerator callGenerator,
                               PlsqlParamBinderGenerator plsqlParamBinderGenerator,
                               OutputRegistrationGenerator outputRegistrationGenerator,
                               ReturnGenerator returnGenerator
    ) {
        String methodWithConnection = generateMethodWithConnectionParam(plsqlCallableAnnotation,
                methodToProcess,
                callGenerator,
                plsqlParamBinderGenerator,
                outputRegistrationGenerator,
                returnGenerator);

        String methodWithoutConnection = generateMethodWithoutConnectionParam(plsqlCallableAnnotation, methodToProcess);

        return GenTools.joinWithReturnToLine(methodWithoutConnection, methodWithConnection);
    }

    private String generateMethodWithConnectionParam(
            PlsqlCallable plsqlCallableAnnotation,
            MethodToProcess methodToProcess,
            CallGenerator callGenerator,
            PlsqlParamBinderGenerator plsqlParamBinderGenerator,
            OutputRegistrationGenerator outputRegistrationGenerator,
            ReturnGenerator returnGenerator
    ) {
        String parameters = extractMethodParameters(methodToProcess);
        String returnType = methodToProcess.method().getReturnType().toString();

        String connectionDeclaration = GenTools.join(java.sql.Connection.class.getCanonicalName(), " ", CNX_VAR);

        String parametersWithConnection = !parameters.isEmpty() ?
                GenTools.join(connectionDeclaration, ",", parameters) : connectionDeclaration;

        var methodInnerTrx = plsqlCallableAnnotation.type() == CallableType.PROCEDURE ? PROCEDURE_METHOD_TEMPLATE : FUNCTION_METHOD_TEMPLATE;

        String initPosition = !parameters.isEmpty() || !isVoid(returnType) ?
                GenTools.assignAndInit(INT, POSITION_VAR, "1") : "";

        String returnStatement = isNullOrEmpty(returnType) || isVoid(returnType) ? "" : GenTools.returnObject(variableName(RETURN_VAR));

        TemplateManager<CodeSnippets.CallableMethodParams> callableMethodTemplateManager = new CodeSnippetsTemplatesManager<>();

        return callableMethodTemplateManager.render(methodInnerTrx, Map.of(
                CodeSnippets.CallableMethodParams.STATEMENT_STATIC_CALL, callGenerator.generate(),
                CodeSnippets.CallableMethodParams.RETURN_TYPE, returnType,
                CodeSnippets.CallableMethodParams.METHOD_NAME, methodToProcess.method().getSimpleName().toString(),
                CodeSnippets.CallableMethodParams.PARAMETERS, parametersWithConnection,
                CodeSnippets.CallableMethodParams.PROCEDURE_FULL_NAME, callGenerator.formatFullNameWithSuffix(),
                CodeSnippets.CallableMethodParams.INIT_POS, initPosition,
                CodeSnippets.CallableMethodParams.STATEMENT_POPULATION, plsqlParamBinderGenerator.generate(),
                CodeSnippets.CallableMethodParams.REGISTER_OUT_PARAM, isVoid(returnType) ? "" : outputRegistrationGenerator.generate(),
                CodeSnippets.CallableMethodParams.RESULT_SET_EXTRACTION, isVoid(returnType) ? "" : returnGenerator.generate(),
                CodeSnippets.CallableMethodParams.RETURN_STATEMENT, returnStatement
        ));
    }

    private String generateMethodWithoutConnectionParam(
            PlsqlCallable plsqlCallableAnnotation,
            MethodToProcess methodToProcess) {

        String paramNames = extractMethodParametersNames(methodToProcess);

        String paramNamesWithConnection = !paramNames.isEmpty() ? GenTools.join(CNX_VAR, ",", " ", paramNames) : CNX_VAR;

        String returnType = methodToProcess.method().getReturnType().toString();

        String methodName = methodToProcess.method().getSimpleName().toString();

        String parameters = extractMethodParameters(methodToProcess);

        String innerMethod = isVoid(returnType) ? GenTools.invokeMethod(methodName, paramNamesWithConnection).concat(";") :
                GenTools.returnObject(GenTools.invokeMethod(methodName, paramNamesWithConnection));

        TemplateManager<CodeSnippets.MethodParams> methodTemplateManager = new CodeSnippetsTemplatesManager<>();

        return methodTemplateManager.render(METHOD_TEMPLATE, Map.of(
                CodeSnippets.MethodParams.RETURN_TYPE, returnType,
                CodeSnippets.MethodParams.METHOD_NAME, methodName,
                CodeSnippets.MethodParams.PARAMETERS, parameters,
                CodeSnippets.MethodParams.DATA_SOURCE, plsqlCallableAnnotation.dataSource(),
                CodeSnippets.MethodParams.TRANSACTIONAL_METHOD, innerMethod
        ));
    }

    private String extractMethodParameters(MethodToProcess methodToProcess) {
        return methodToProcess.method().getParameters().stream().map(v -> String.format("%s %s", v.asType(), v.getSimpleName()))
                .collect(Collectors.joining(", "));
    }

    private String extractMethodParametersNames(MethodToProcess methodToProcess) {
        return methodToProcess.method().getParameters().stream().map(VariableElement::getSimpleName)
                .collect(Collectors.joining(", "));
    }

}
