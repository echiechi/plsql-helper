package com.plsql.tools.tools.fields;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.mapping.ObjectField;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.*;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.ValidationUtils.isValidGetter;

public class FieldMethodMapper {

    private final ProcessingContext processingContext;

    public FieldMethodMapper(ProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    public FieldMappingResult extractFieldMethodMap(String paramName, TypeElement paramClass) {
        if (paramName == null || paramName.trim().isEmpty()) {
            processingContext.logError("Parameter name cannot be null or empty");
            return FieldMappingResult.failure("Invalid parameter name");
        }

        if (paramClass == null) {
            processingContext.logError("Parameter class cannot be null");
            return FieldMappingResult.failure("Invalid parameter class");
        }

        Map<ObjectField, Element> fieldMethodMap = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        List<Element> fields = getFieldElements(paramClass);
        List<Element> methods = getMethodElements(paramClass);

        for (Element field : fields) {
            processField(paramName, field, methods, fieldMethodMap, warnings);
        }

        warnings.forEach(processingContext::logWarning);

        return FieldMappingResult.success(fieldMethodMap, warnings);
    }

    private List<Element> getFieldElements(TypeElement paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    private List<Element> getMethodElements(TypeElement paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .collect(Collectors.toList());
    }

    private void processField(String paramName, Element field, List<Element> methods,
                              Map<ObjectField, Element> fieldMethodMap, List<String> warnings) {
        String fieldName = extractNameAsStr(field);

        Optional<Element> matchingGetter = findMatchingGetter(fieldName, methods);

        if (matchingGetter.isPresent()) {
            fieldMethodMap.put(new ObjectField(paramName, field), matchingGetter.get());
        } else if (isModifierPresent(field, Modifier.PUBLIC)) {
            fieldMethodMap.put(new ObjectField(paramName, field), null);
            warnings.add("Using direct field access for: " + fieldName + " (no getter found)");
        } else {
            warnings.add("Skipping private field without getter: " + fieldName);
        }
    }

    private Optional<Element> findMatchingGetter(String fieldName, List<Element> methods) {
        if (fieldName == null || fieldName.isEmpty()) {
            return Optional.empty();
        }

        String lowerFieldName = fieldName.toLowerCase();

        return methods.stream()
                .filter(method -> isValidGetter(extractNameAsStr(method)))
                .filter(method -> {
                    String methodName = extractNameAsStr(method).toLowerCase();
                    return methodName.endsWith(lowerFieldName);
                })
                .findFirst();
    }

    public static String extractNameAsStr(Element element) {
        if (element.getSimpleName() == null) {
            return "";
        }
        return element.getSimpleName().toString();
    }

    public static boolean isModifierPresent(Element element, Modifier modifier) {
        return element != null &&
                modifier != null &&
                element.getModifiers().contains(modifier);
    }
}
