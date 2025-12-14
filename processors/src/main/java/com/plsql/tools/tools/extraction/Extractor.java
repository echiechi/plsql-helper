package com.plsql.tools.tools.extraction;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.annotations.PlsqlParam;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.extraction.info.*;
import com.plsql.tools.utils.CaseConverter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.Tools.RETURN_NAME;
import static com.plsql.tools.tools.Tools.extractNameAsStr;
import static com.plsql.tools.tools.ValidationUtils.isValidGetter;
import static com.plsql.tools.tools.ValidationUtils.isValidSetter;
import static com.plsql.tools.utils.CaseConverter.upperCaseFirstLetter;

public class Extractor {

    public static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
    public static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
    public static final String POS = "pos";
    private static Extractor INSTANCE;
    private final Map<TypeMirror, List<AttachedElementInfo>> cache = new HashMap<>();
    private ProcessingContext context;

    private Extractor() {
    }

    public static Extractor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Extractor();
        }
        return INSTANCE;
    }

    public void context(ProcessingContext context) {
        if (this.context == null) {
            this.context = context;
        } else {
            throw new IllegalStateException("Context is already set");
        }
    }

    public ProcessingContext context() {
        if (this.context == null) {
            throw new IllegalStateException("Context is not yet defined");
        }
        return this.context;
    }

    public List<AttachedElementInfo> getAttachedElements(TypeMirror typeMirror) {
        return cache.get(typeMirror);
    }

    public boolean isCollection(TypeMirror type) {
        var types = context().getProcessingEnv().getTypeUtils();
        var elements = context().getProcessingEnv().getElementUtils();
        TypeMirror collectionMirror =
                elements.getTypeElement(JAVA_UTIL_COLLECTION).asType();
        return types.isAssignable(types.erasure(type), types.erasure(collectionMirror));
    }

    public boolean isOptional(TypeMirror type) {
        var types = context().getProcessingEnv().getTypeUtils();
        var elements = context().getProcessingEnv().getElementUtils();
        TypeMirror optionalMirror =
                elements.getTypeElement(JAVA_UTIL_OPTIONAL).asType();
        return types.isAssignable(types.erasure(type), types.erasure(optionalMirror));
    }

    public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
        String returnTypeStr = method.getReturnType().toString();
        if (Tools.isVoid(returnTypeStr)) {
            return null;
        }
        var outputs = Arrays.asList(method.getAnnotation(PlsqlCallable.class).outputs());
        var returnType = (DeclaredType) method.getReturnType();
        if (outputs.size() == 1) {
            return List.of(handleOutput(returnType, outputs.get(0)));
        } else if (outputs.size() > 1) {
            List<AttachedElementInfo> attachedElementInfoList = cache.get(returnType.asElement().asType());
            List<ReturnElementInfo> returnElementInfoList = new ArrayList<>();
            TypeInfo typeInfo = extractTypeInfo(returnType);
            ElementInfo elementInfo = new ElementInfo(typeInfo, RETURN_NAME);
            for (Output output : outputs) {
                Optional<AttachedElementInfo> correspondingElementOpt = attachedElementInfoList.stream()
                        .filter(e -> e.getName().equals(output.field()))
                        .findAny();
                correspondingElementOpt
                        .ifPresent(attachedElementInfo -> {
                            var returnElementInfo = handleOutput(attachedElementInfo, output);
                            returnElementInfo.setParent(elementInfo);
                            returnElementInfoList.add(returnElementInfo);
                        });
            }
            return returnElementInfoList;
        } else {
            throw new IllegalStateException("@Output is not defined but a return type is present");
        }
    }

    // TODO : refactor
    private ReturnElementInfo handleOutput(AttachedElementInfo attachedElementInfo, Output output) {
        var typeInfo = attachedElementInfo.getTypeInfo();
        if (typeInfo.isSimple()) {
            var type = typeInfo.asJdbcHelper();
            if (type != null) {
                var returnElementInfo = new ReturnElementInfo(typeInfo, attachedElementInfo.getName(),
                        output, POS
                );
                if (!output.field().isBlank()) {
                    returnElementInfo.setName(output.field());
                }
                return returnElementInfo;
            } else {
                throw new IllegalStateException("Type info is not simple " + typeInfo);
            }
        } else if (typeInfo.isWrapped()) {
            ComposedElementInfo composedElementInfo = convertInto(typeInfo.getRawWrappedType());
            composedElementInfo.setTypeInfo(typeInfo);
            if (!output.field().isBlank()) {
                composedElementInfo.setName(output.field());
            }
            composedElementInfo.setName(attachedElementInfo.getName());
            return new ReturnElementInfo(composedElementInfo, output, POS);
        } else {
            ComposedElementInfo composedElementInfo = convertInto(typeInfo.getRawType());
            if (!output.field().isBlank()) {
                composedElementInfo.setName(output.field());
            }
            composedElementInfo.setName(attachedElementInfo.getName());
            return new ReturnElementInfo(composedElementInfo, output, POS);
        }
    }

    private ReturnElementInfo handleOutput(DeclaredType returnType, Output output) { // TODO : handle wrapper 
        var type = JdbcHelper.fromSimpleName(returnType.toString());
        TypeInfo typeInfo = extractTypeInfo(returnType);

        if (type != null) { // simple type
            var returnElementInfo = new ReturnElementInfo(typeInfo, RETURN_NAME,
                    output, POS
            );
            if (!output.field().isBlank()) {
                returnElementInfo.setName(output.field());
            }
            return returnElementInfo;
        } else if (typeInfo.isWrapped()) {
            ComposedElementInfo composedElementInfo = convertInto(typeInfo.getRawWrappedType());
            composedElementInfo.setTypeInfo(typeInfo);
            if (!output.field().isBlank()) {
                composedElementInfo.setName(output.field());
            }
            composedElementInfo.setName(RETURN_NAME);
            return new ReturnElementInfo(composedElementInfo, output, POS);
        } else {
            ComposedElementInfo composedElementInfo = convertInto(returnType.asElement(), typeInfo);
            if (!output.field().isBlank()) {
                composedElementInfo.setName(output.field());
            }
            composedElementInfo.setName(RETURN_NAME);
            return new ReturnElementInfo(composedElementInfo, output, POS);
        }
    }

    public List<String> extractPramNames(List<ElementInfo> elementInfoList) {
        List<String> paramNames = new ArrayList<>();
        for (var elementInfo : elementInfoList) {
            if (elementInfo.getTypeInfo().isSimple()) {
                paramNames.add(extractName(elementInfo));
            } else {
                paramNames.addAll(extractNestedParamNames(elementInfo.getTypeInfo().getMirror()));
            }
        }
        return paramNames;
    }

    private List<String> extractNestedParamNames(TypeMirror attachedElement) {
        List<String> paramNames = new ArrayList<>();
        List<AttachedElementInfo> nestedAttachedElements = cache.get(attachedElement);
        if (nestedAttachedElements != null) {
            for (var innerElement : nestedAttachedElements) {
                if (innerElement.getTypeInfo().isSimple()) {
                    paramNames.add(extractName(innerElement));
                } else {
                    var type = innerElement.getTypeInfo().getMirror();
                    if (innerElement.getTypeInfo().isWrapped()) {
                        type = innerElement.getTypeInfo().getWrappedType();
                    }
                    paramNames.addAll(extractNestedParamNames(type));
                }
            }
        }
        return paramNames;
    }

    private String extractName(ElementInfo elementInfo) {
        PlsqlParam param = elementInfo.getTypeInfo().getRawType().getAnnotation(PlsqlParam.class);
        if (param != null) {
            return param.value();
        } else {
            return CaseConverter.toSnakeCase(elementInfo.getName());
        }
    }

    public List<ElementInfo> extractParams(ExecutableElement method) {
        List<ElementInfo> elementInfoList = new ArrayList<>();
        for (var parameter : method.getParameters()) {
            var paramName = parameter.getSimpleName().toString();
            TypeInfo typeInfo = extractTypeInfo(parameter);
            ElementInfo elementInfo = new ElementInfo(typeInfo, paramName);
            if (typeInfo.isSimple()) {
                elementInfoList.add(elementInfo);
            } else {
                elementInfoList.add(convertInto(parameter));
            }
        }
        return elementInfoList;
    }

    public ComposedElementInfo convertInto(Element record) {
        if (record == null) {
            throw new IllegalArgumentException("@Record class cannot be null");
        }
        var typeInfo = extractTypeInfo(record);
        return convertInto(record, typeInfo);
    }

    public ComposedElementInfo convertInto(Element record, TypeInfo typeInfo) {
        ComposedElementInfo composedElementInfo = new ComposedElementInfo(typeInfo);
        composedElementInfo.setName(record.getSimpleName().toString());
        if (typeInfo.isWrapped()) {
            composedElementInfo.setDefaultConstructor(extractConstructor(typeInfo.getRawWrappedType()));
        } else {
            composedElementInfo.setDefaultConstructor(extractConstructor(record));
        }

        if (cache.containsKey(record.asType())) {
            for (AttachedElementInfo attachedElement : cache.get(record.asType())) {
                composedElementInfo.addElement(attachedElement);
                if (!attachedElement.getTypeInfo().isSimple()) {
                    populateNestedObjects(composedElementInfo, attachedElement.getTypeInfo().getMirror());
                }
            }
        }
        return composedElementInfo;
    }

    private void populateNestedObjects(ComposedElementInfo composedElementInfo, TypeMirror attachedElement) {
        List<AttachedElementInfo> nestedAttachedElements = cache.get(attachedElement);
        if (nestedAttachedElements != null) {
            composedElementInfo.addNestedElement(attachedElement, nestedAttachedElements);
            for (var element : nestedAttachedElements) {
                if (!element.getTypeInfo().isSimple()) {
                    var type = element.getTypeInfo().getMirror();
                    if (element.getTypeInfo().isWrapped()) {
                        type = element.getTypeInfo().getWrappedType();
                    }
                    populateNestedObjects(composedElementInfo, type);
                }
            }
        }
    }

    public List<AttachedElementInfo> extractClassInfo(Element record) {
        if (record == null) {
            throw new IllegalArgumentException("@Record class cannot be null");
        }
        if (cache.containsKey(record.asType())) {
            return cache.get(record.asType());
        }
        List<Element> fields = extractClassFields(record);
        List<ExecutableElement> methods = extractClassMethod(record);
        List<ExecutableElement> constructors = extractClassConstructor(record);
        List<AttachedElementInfo> attachedElements = new ArrayList<>();
        for (Element field : fields) {
            var attachedElement = new AttachedElementInfo();
            attachedElement.setName(extractNameAsStr(field));
            if (record.getKind() == ElementKind.RECORD) {
                handleRecord(attachedElement, methods);
            } else if (record.getKind() == ElementKind.CLASS) {
                attachedElement.setPublic(Tools.isModifierPresent(field, Modifier.PUBLIC));
                handleClass(attachedElement, constructors, methods);
            } else {
                throw new IllegalStateException("Unsupported Element Kind");
            }
            // extract constructor
            var typeInfo = extractTypeInfo(field);
            attachedElement.setTypeInfo(typeInfo);
            if (!typeInfo.isWrapped() && !typeInfo.isSimple()) {
                attachedElement.setDefaultConstructor(extractConstructor(field));
            } else if (typeInfo.isWrapped() && !TypeInfo.isSimple(typeInfo.getWrappedType())) {
                attachedElement.setDefaultConstructor(extractConstructor(typeInfo.getRawWrappedType()));
            }
            attachedElements.add(attachedElement);
        }
        cache.put(record.asType(), attachedElements);
        return attachedElements;
    }

    public TypeInfo extractTypeInfo(Element field) {
        var typeInfo = new TypeInfo();
        typeInfo.setMirror(field.asType());
        typeInfo.setRawType(field);
        typeInfo.setWrappedType(extractDeclaredType(field.asType()));
        if (typeInfo.getWrappedType() != null) {
            var types = context().getProcessingEnv().getTypeUtils();
            typeInfo.setRawWrappedType(types.asElement(typeInfo.getWrappedType()));
        }
        return typeInfo;
    }

    public TypeInfo extractTypeInfo(DeclaredType field) {
        var typeInfo = new TypeInfo();
        typeInfo.setMirror(field);
        typeInfo.setRawType(field.asElement());
        typeInfo.setWrappedType(extractDeclaredType(field));
        if (typeInfo.getWrappedType() != null) {
            var types = context().getProcessingEnv().getTypeUtils();
            typeInfo.setRawWrappedType(types.asElement(typeInfo.getWrappedType()));
        }
        return typeInfo;
    }

    public TypeMirror extractDeclaredType(TypeMirror asType) {
        if (asType.getKind() == TypeKind.DECLARED) {
            var typeMirror = ((DeclaredType) asType).getTypeArguments()
                    .stream()
                    .findFirst();
            if (typeMirror.isPresent()) {
                return typeMirror.get();
            }
        }
        return null;
    }

    private void handleRecord(AttachedElementInfo attachedElement, List<ExecutableElement> methods) {
        Optional<ExecutableElement> matchingGetter = findMatchingMethod(attachedElement.getName(),
                methods,
                method -> extractNameAsStr(method).equals(attachedElement.getName())
        );
        matchingGetter.ifPresent(attachedElement::setGetter); // should always be present
    }

    private void handleClass(AttachedElementInfo attachedElement,
                             List<ExecutableElement> constructors,
                             List<ExecutableElement> methods
    ) {
        var constructor = constructors.stream()
                .filter(c -> c.getParameters().isEmpty())
                .findFirst();
        if (constructor.isEmpty()) {
            throw new IllegalStateException("No default constructor is present");
        }
        Optional<ExecutableElement> matchingGetter = findMatchingMethod(attachedElement.getName(),
                methods,
                method -> isValidGetter(extractNameAsStr(method))
        );
        Optional<ExecutableElement> matchingSetter = findMatchingMethod(attachedElement.getName(),
                methods,
                method -> isValidSetter(extractNameAsStr(method))
        );
        if (matchingGetter.isPresent()) {
            attachedElement.setGetter(matchingGetter.get());
        } else if (attachedElement.isPublic()) {
            attachedElement.setPublic(true);
            context.logWarning("Using direct field access for: " + attachedElement.getName() + " (no getter found)");
        } else {
            context.logWarning("Skipping private field without getter: " + attachedElement.getName());
        }
        matchingSetter.ifPresent(attachedElement::setSetter);
    }

    private List<Element> extractClassFields(Element record) {
        return record.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    private List<ExecutableElement> extractClassMethod(Element paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toList());
    }

    public ExecutableElement extractConstructor(Element paramClass) {
        if (paramClass.getKind() == ElementKind.FIELD || paramClass.getKind() == ElementKind.PARAMETER) {
            paramClass = context.getProcessingEnv().getTypeUtils().asElement(paramClass.asType());
        }
        if (paramClass.getKind() == ElementKind.RECORD) {
            return extractClassConstructor(paramClass).get(0);
        } else if (paramClass.getKind() == ElementKind.CLASS) {
            return extractClassConstructor(paramClass).stream()
                    .filter(c -> c.getParameters().isEmpty())
                    .findFirst().orElseThrow(() -> new NoSuchMethodError("No Default constructor provided for class"));
        } else {
            throw new IllegalStateException("Unsupported Element Kind " + paramClass.getKind());
        }
    }

    public List<ExecutableElement> extractClassConstructor(Element paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toList());
    }

    private Optional<ExecutableElement> findMatchingMethod(String fieldName,
                                                           List<ExecutableElement> methods,
                                                           Predicate<ExecutableElement> check) {
        if (fieldName == null || fieldName.isEmpty()) {
            return Optional.empty();
        }
        return methods.stream()
                .filter(check)
                .filter(method -> {
                    String methodName = extractNameAsStr(method);
                    return methodName.endsWith(upperCaseFirstLetter(fieldName));
                })
                .map(ExecutableElement.class::cast)
                .findFirst();
    }
}
