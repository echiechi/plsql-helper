package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.ElementFinder;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.fields.ExtractedField;
import com.plsql.tools.tools.fields.FieldMappingResult;
import com.plsql.tools.tools.fields.FieldMethodMapper;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.*;

public class MethodProcessor {
    private final ProcessingContext context;
    private final FieldMethodMapper extractor;
    private final ElementFinder elementFinder;

    public MethodProcessor(ProcessingContext context, RoundEnvironment roundEnv) {
        this.context = context;
        this.extractor = new FieldMethodMapper(context);
        this.elementFinder = new ElementFinder(roundEnv);
    }

    // TODO : handle no args and all args constructor
    // TODO : handle List stream and ResultSet ?
    public MethodProcessingResult process(ExecutableElement method) {
        return new MethodProcessingResult(processParameters(method), processReturnType(method));
    }

    // TODO : cache extracted parameters / classes
    // TODO : enhance the method can only handle collections for now needs to change
    // TODO : for now I should only focus on single OUT from procstock next I should focus on multiple OUT
    private MethodProcessingResult.ReturnResult processReturnType(ExecutableElement method) {
        context.logInfo("------------>" + method.getReturnType().toString());
        String returnType = method.getReturnType().toString();
        var returnResult = new MethodProcessingResult.ReturnResult();
        if (Tools.isVoid(returnType)) {
            returnResult.setValid(true);
            return returnResult;
        }

        var declaredType = (DeclaredType) method.getReturnType();
        returnResult.addType(declaredType);
        JdbcHelper simpleType = JdbcHelper.fromSimpleName(declaredType.asElement().toString());
        if (simpleType != null && simpleType.isCollection()) {
            context.logInfo("Processing collection");
            Set<String> usedClasses = new HashSet<>();
            if (simpleType == JdbcHelper.LIST || simpleType == JdbcHelper.SET) { // TODO : handle Collection type ?
                ((DeclaredType) method.getReturnType()).getTypeArguments()
                        .forEach(e -> {
                            processClassFields(((DeclaredType) e).asElement(), e.toString(), usedClasses, returnResult.getByType(declaredType));
                        });
            }
        } else if (simpleType != null && simpleType.isPrimitive()) {
            context.logInfo("Processing simple type");
        } else {
            context.logInfo("Processing Object");
        }
        context.logInfo("Return processing result : " + returnResult);
        return returnResult;
    }

    private MethodProcessingResult.ParameterResult processParameters(ExecutableElement method) {
        List<String> parameters = new ArrayList<>();
        Set<ExtractedField> extractedFields = new LinkedHashSet<>();

        Set<String> usedClasses = new HashSet<>();
        boolean isValid = true;

        for (var parameter : method.getParameters()) {
            String paramType = parameter.asType().toString();
            String paramName = parameter.getSimpleName().toString();
            parameters.add(String.format("%s %s", paramType, paramName));

            JdbcHelper type = JdbcHelper.fromSimpleName(paramType);
            if (type != null) {
                // Simple parameter
                extractedFields.add(new ExtractedField("", parameter));
            } else {
                if (!processClassFields(parameter, paramType, usedClasses, extractedFields)) {
                    isValid = false;
                } else {
                    usedClasses.add(paramType);
                }
            }
        }
        var parameterResult = new MethodProcessingResult.ParameterResult(parameters, extractedFields);
        parameterResult.setValid(isValid); // TODO : remove is valid ?
        return parameterResult;
    }

    private boolean processClassFields(Element parameter, String paramType,
                                       Set<String> usedClasses, Set<ExtractedField> extractedSet) {
        if (usedClasses.contains(paramType)) {
            context.logError("Duplicate input type detected: " + paramType);
            return false;
        }

        TypeElement paramClass = elementFinder.findElementByName(paramType).orElse(null);
        if (paramClass == null) {
            context.logError("Cannot find parameter class: " + paramType);
            return false;
        }
        FieldMappingResult fieldMappingResult = extractor
                .extractFields(parameter.getSimpleName().toString(), paramClass);
        if (fieldMappingResult.isSuccess()) {
            context.logWarnings(fieldMappingResult.getWarnings());
            extractedSet.addAll(fieldMappingResult.getFieldMethodMap());
        } else {
            context.logError(fieldMappingResult.getErrorMessage());
            return false;
        }
        return true;
    }
}
