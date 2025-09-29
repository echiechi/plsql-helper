package com.plsql.tools.tools.fields;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.ElementTools;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.ObjectInfo;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.Tools.extractNameAsStr;
import static com.plsql.tools.tools.ValidationUtils.isValidGetter;
import static com.plsql.tools.tools.ValidationUtils.isValidSetter;

public class FieldMethodExtractor {

    private static FieldMethodExtractor INSTANCE;

    private final ProcessingContext context;

    private final Map<TypeElement, Set<FieldInfo>> recordsCache;

    public final ElementTools elementTools;

    private FieldMethodExtractor(ProcessingContext context) {
        this.context = context;
        this.recordsCache = new HashMap<>();
        this.elementTools = new ElementTools(context);
    }

    public static FieldMethodExtractor getInstance(ProcessingContext context) {
        if (INSTANCE == null) {
            INSTANCE = new FieldMethodExtractor(context);
        }
        return INSTANCE;
    }

    public ObjectInfo extractFields(String paramName, TypeElement paramClass) {
        if (paramName == null || paramName.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }

        if (paramClass == null) {
            throw new IllegalArgumentException("Parameter class cannot be null");
        }

        ObjectInfo objectInfo = new ObjectInfo(paramName, paramClass);
        Set<FieldInfo> fieldInfoSet; // TODO : handle lists
        if (!recordsCache.containsKey(paramClass)) {
            fieldInfoSet = extractClassInfo(paramClass);
        } else {
            fieldInfoSet = recordsCache.get(paramClass);
        }
        objectInfo.setFieldInfoSet(fieldInfoSet);
        populateNestedObjects(objectInfo, fieldInfoSet);
        return objectInfo;
    }

    private void populateNestedObjects(ObjectInfo objectInfo, Set<FieldInfo> fieldInfoSet) {
        if (fieldInfoSet != null)
            for (var field : fieldInfoSet) {
                if (!field.isSimple()) {
                    TypeElement typeElement = Tools.getTypeElement(context, field.getField());
                    if (elementTools.isWrapped(typeElement)) {
                        /*var asType = field.getField().asType();
                        if (asType.getKind() == TypeKind.DECLARED) {
                            var typeMirror = ((DeclaredType) asType).getTypeArguments()
                                    .stream()
                                    .findFirst();
                            if (typeMirror.isPresent()) {
                                typeElement = Tools.getTypeElement(context, typeMirror.get());
                            }
                        }*/
                        typeElement = elementTools.extractDeclaredType(field.getField().asType());
                    }
                    objectInfo.addToNestedObjects(typeElement, recordsCache.get(typeElement));
                    populateNestedObjects(objectInfo, recordsCache.get(typeElement));
                }
            }
    }

    public Set<FieldInfo> extractClassInfo(TypeElement record) {
        if (record == null) {
            throw new IllegalArgumentException("Parameter class cannot be null");
        }

        if (recordsCache.containsKey(record)) {
            context.logInfo("Fetch from cache", record);
            return recordsCache.get(record);
        }

        Set<FieldInfo> fieldSet = new LinkedHashSet<>();
        List<Element> fields = getFieldElements(record);
        List<ExecutableElement> methods = getMethodElements(record);

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
            FieldInfo extractedField = null;
            if (matchingGetter.isPresent()) {
                extractedField = new FieldInfo((VariableElement) field, matchingGetter.get());
            } else if (Tools.isModifierPresent(field, Modifier.PUBLIC)) {
                extractedField = new FieldInfo((VariableElement) field);
                context.logWarning("Using direct field access for: " + fieldName + " (no getter found)");
            } else {
                context.logWarning("Skipping private field without getter: " + fieldName);
            }
            if (extractedField != null && matchingSetter.isPresent()) {
                extractedField.setSetter(matchingSetter.get());
            }
            fieldSet.add(extractedField);
        }
        recordsCache.put(record, fieldSet);
        return fieldSet;
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

    private boolean isFieldTypeAnnotated(VariableElement fieldElement, String annotationName) {
        Element typeElement = toTypeElement(fieldElement);
        if (typeElement == null) {
            return false; // Could be a primitive or something unresolved
        }
        // Step 3: Check annotations on the type
        return typeElement.getAnnotationMirrors().stream()
                .anyMatch(a -> ((TypeElement) a.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(annotationName));
    }

    private TypeElement toTypeElement(VariableElement fieldElement) {
        TypeMirror fieldType = fieldElement.asType();
        // Step 2: Get the element representing that type
        Element typeElement = context.getProcessingEnv().getTypeUtils().asElement(fieldType);
        return (TypeElement) typeElement;
    }
}
