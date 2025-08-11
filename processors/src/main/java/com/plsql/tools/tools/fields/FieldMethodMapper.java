package com.plsql.tools.tools.fields;

import com.plsql.tools.ProcessingContext;

import javax.lang.model.element.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.ValidationUtils.isValidGetter;
import static com.plsql.tools.tools.ValidationUtils.isValidSetter;

public class FieldMethodMapper {

    private final ProcessingContext processingContext;

    public FieldMethodMapper(ProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    public FieldMappingResult extractFields(String paramName, TypeElement paramClass) {
        if (paramName == null || paramName.trim().isEmpty()) {
            processingContext.logError("Parameter name cannot be null or empty");
            return FieldMappingResult.failure("Invalid parameter name");
        }

        if (paramClass == null) {
            processingContext.logError("Parameter class cannot be null");
            return FieldMappingResult.failure("Invalid parameter class");
        }

        Set<ExtractedField> fieldSet = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();

        List<Element> fields = getFieldElements(paramClass);
        List<ExecutableElement> methods = getMethodElements(paramClass);

        for (Element field : fields) {
            String fieldName = extractNameAsStr(field);
            Optional<ExecutableElement> matchingGetter = findMatchingMethod(fieldName,
                    methods,
                    method -> isValidGetter(extractNameAsStr(method))
            );
            Optional<ExecutableElement> matchingSetter = findMatchingMethod(fieldName,
                    methods,
                    method -> isValidSetter(extractNameAsStr(method))
            );
            ExtractedField extractedField = null;
            if (matchingGetter.isPresent()) {
                extractedField = new ExtractedField(paramName, field, matchingGetter.get());
            } else if (isModifierPresent(field, Modifier.PUBLIC)) {
                extractedField = new ExtractedField(paramName, field);
                warnings.add("Using direct field access for: " + fieldName + " (no getter found)");
            } else {
                warnings.add("Skipping private field without getter: " + fieldName);
            }
            if (extractedField != null && matchingSetter.isPresent()) {
                extractedField.setSetter(matchingSetter.get());
            }
            fieldSet.add(extractedField);
        }

        warnings.forEach(processingContext::logWarning);
        return FieldMappingResult.success(fieldSet, warnings);
    }

    private List<Element> getFieldElements(TypeElement paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    private List<ExecutableElement> getMethodElements(TypeElement paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toList());
    }

    private Optional<ExecutableElement> findMatchingMethod(String fieldName,
                                                           List<ExecutableElement> methods,
                                                           Predicate<ExecutableElement> check) {
        if (fieldName == null || fieldName.isEmpty()) {
            return Optional.empty();
        }

        String lowerFieldName = fieldName.toLowerCase();

        return methods.stream()
                .filter(check)
                .filter(method -> {
                    String methodName = extractNameAsStr(method).toLowerCase();
                    return methodName.endsWith(lowerFieldName);
                })
                .map(ExecutableElement.class::cast)
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
