package com.plsql.tools.processors.params;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.JavaToSql;
import com.plsql.tools.mapping.ObjectField;
import com.plsql.tools.tools.ElementFinder;
import com.plsql.tools.tools.fields.FieldMethodMapper;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

public class MethodParameterProcessor {
    private final ProcessingContext context;
    private final RoundEnvironment roundEnv;
    private final FieldMethodMapper mapper;
    private final ElementFinder elementFinder;

    public MethodParameterProcessor(ProcessingContext context, RoundEnvironment roundEnv) {
        this.context = context;
        this.roundEnv = roundEnv;
        this.mapper = new FieldMethodMapper(context);
        this.elementFinder = new ElementFinder(roundEnv);
    }

    public ProcessingResult processParameters(ExecutableElement method) {
        List<String> parameters = new ArrayList<>();
        Map<ObjectField, Element> extractedMap = new LinkedHashMap<>();
        Set<String> usedClasses = new HashSet<>();
        boolean isValid = true;

        for (var parameter : method.getParameters()) {
            String paramType = parameter.asType().toString();
            String paramName = parameter.getSimpleName().toString();

            parameters.add(String.format("%s %s", paramType, paramName));

            JavaToSql type = JavaToSql.fromSimpleName(paramType);
            if (type != null) {
                extractedMap.put(new ObjectField("", parameter), null);
            } else {
                if (!processComplexParameter(parameter, paramType, usedClasses, extractedMap)) {
                    isValid = false;
                }
            }
        }

        return new ProcessingResult(parameters, extractedMap, isValid);
    }

    private boolean processComplexParameter(Element parameter, String paramType,
                                            Set<String> usedClasses, Map<ObjectField, Element> extractedMap) {
        if (usedClasses.contains(paramType)) {
            context.logError("Duplicate input type detected: " + paramType);
            return false;
        }

        TypeElement paramClass = elementFinder.findElementByName(paramType).orElse(null);
        if (paramClass == null) {
            context.logError("Cannot find parameter class: " + paramType);
            return false;
        }

        usedClasses.add(paramType);

        extractedMap.putAll(mapper.extractFieldMethodMap(parameter.getSimpleName().toString(), paramClass).getFieldMethodMap());
        return true;

    }
}
