package com.plsql.tools.tools;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.MultiOutput;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.tools.fields.info.ObjectInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class ElementTools {
    public static class OutputElement {
        public Output output;
        public Element element;
        public String pos;
        public boolean isFieldOutput = false;
        public TypeElement wrappedType;

        public OutputElement(Output output, Element element) {
            this.output = output;
            this.element = element;
        }

        public boolean isWrapped() {
            return wrappedType != null;
        }

        @Override
        public String toString() {
            return "OutputElement{" +
                    "output=" + output +
                    ", element=" + element +
                    ", pos='" + pos + '\'' +
                    ", isFieldOutput=" + isFieldOutput +
                    '}';
        }
    }

    private final ProcessingContext context;

    private Pattern pattern = Pattern.compile("value\\s*=\\s*\"([^\"]+)\".*field\\s*=\\s*\"([^\"]+)\"");

    public ElementTools(ProcessingContext context) {
        this.context = context;
    }

    public boolean implementsInterface(Element element, Class<?> interfaceClass) {
        var typeUtils = context.getProcessingEnv().getTypeUtils();
        var elementUtils = context.getProcessingEnv().getElementUtils();
        if (!(element instanceof TypeElement typeElement)) {
            return false;
        }

        TypeMirror elementType = typeElement.asType();

        // Get the TypeMirror for the target interface
        TypeElement interfaceElement = elementUtils.getTypeElement(interfaceClass.getCanonicalName());
        if (interfaceElement == null) {
            return false; // interface not found
        }

        TypeMirror interfaceType = interfaceElement.asType();

        // Check assignability
        return typeUtils.isAssignable(elementType, interfaceType);
    }

    public boolean isSimpleWrapper(Element element) {
        return implementsInterface(element, java.util.Set.class) || implementsInterface(element, java.util.List.class)
                || implementsInterface(element, java.util.Optional.class);
    }

    public boolean isMapWrapper(Element element) {
        return implementsInterface(element, java.util.Map.class);
    }

    public boolean isWrapped(Element element) {
        return isMapWrapper(element) || isSimpleWrapper(element);
    }

    public boolean isIterator(Element element) {
        return implementsInterface(element, java.lang.Iterable.class);
    }

    public boolean isOptional(Element element) {
        return implementsInterface(element, java.util.Optional.class);
    }

    public boolean isCollection(Element element) {
        return implementsInterface(element, java.util.Set.class) || implementsInterface(element, java.util.List.class);
    }

    public TypeElement mapToConcreteClass(Element element) {
        if (!isCollection(element)) {
            throw new IllegalArgumentException("Element must be a collection");
        }
        if (element.getKind() == ElementKind.CLASS) {
            return (TypeElement) element;
        } else if (implementsInterface(element, java.util.List.class)) {
            return context.getProcessingEnv()
                    .getElementUtils()
                    .getTypeElement("java.util.ArrayList");
        } else if (implementsInterface(element, java.util.Set.class)) {
            return context.getProcessingEnv()
                    .getElementUtils()
                    .getTypeElement("java.util.HashSet");
        }
        return null;
    }

    public ExecutableElement extractConstructor(Element element) {
        var typeElement = Tools.getTypeElement(context, element);
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructor = (ExecutableElement) enclosed;
                if (constructor.getParameters() == null || constructor.getParameters().isEmpty()) {
                    return constructor;
                }
            }
        }
        return null;
    }

    public List<OutputElement> extractOutputs(VariableInfo variableInfo) {
        List<OutputElement> outputs = new ArrayList<>();
        if (variableInfo == null) {
            return outputs;
        }
        if (variableInfo instanceof ObjectInfo objectInfo) { // TODO: in case of Multioutput must be >= 2 and refactor
            if (objectInfo.getOutputs() != null && objectInfo.getOutputs().size() == 1) {
                outputs.add(new OutputElement(objectInfo.getOutputs().get(0), objectInfo.getField()));
            } else if (objectInfo.getOutputs() != null && objectInfo.getOutputs().size() >= 2) {
                for (var out : objectInfo.getOutputs()) {
                    var outputFieldOpt = objectInfo
                            .getFieldInfoSet()
                            .stream()
                            .filter(f -> f.getName().equals(out.field()))
                            .findFirst();
                    if (outputFieldOpt.isPresent()) {
                        var outputElement = new OutputElement(out, outputFieldOpt.get().getField());
                        outputElement.isFieldOutput = true;
                        var typeElement = Tools.getTypeElement(context, outputElement.element);
                        if (isWrapped(typeElement)) {
                            outputElement.wrappedType = extractDeclaredType(outputElement.element.asType());
                        }
                        outputs.add(outputElement);
                    }
                }
            }
        } else if (variableInfo.getOutputs() != null) {
            outputs.add(new OutputElement(variableInfo.getOutputs().get(0), variableInfo.getField()));
        }
        context.logDebug("Extracted Output elements: ", outputs);
        return outputs;
    }

    public TypeElement extractDeclaredType(TypeMirror asType) {
        if (asType.getKind() == TypeKind.DECLARED) {
            var typeMirror = ((DeclaredType) asType).getTypeArguments()
                    .stream()
                    .findFirst();
            if (typeMirror.isPresent()) {
                return Tools.getTypeElement(context, typeMirror.get());
            }
        }
        return null;
    }

    public Output output(String value) {
        return new Output() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Output.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String field() {
                return "";
            }
        };
    }

    public Output output(String value, String field) {
        return new Output() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Output.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String field() {
                return field;
            }
        };
    }
    // TODO: refactor
    @Deprecated
    public List<Output> extractAnnotationsFromReturn(ExecutableElement method) {
        List<Output> list = new ArrayList<>();
        var returnAnnotations = method
                .getReturnType()
                .getAnnotationMirrors();
        var multiOutput = returnAnnotations
                .stream()
                .filter(m -> m.toString().startsWith("@" + MultiOutput.class.getCanonicalName()))
                .findFirst();
        if (multiOutput.isPresent()) {
            multiOutput.get().getElementValues()
                    .values()
                    .forEach(v -> {
                        if (v.getValue() instanceof List<?>) {
                            ((List<?>) v.getValue()).forEach(val -> {
                                Matcher matcher = pattern.matcher(val.toString());
                                if (matcher.find()) {
                                    String value = matcher.group(1);
                                    String field = matcher.group(2);
                                    list.add(output(value, field));
                                }
                            });
                        }
                    });
        } else {
            var oneOutput = returnAnnotations
                    .stream()
                    .filter(m -> m.toString().startsWith("@" + Output.class.getCanonicalName()))
                    .findFirst();
            if (oneOutput.isPresent()) {
                var values = oneOutput.get()
                        .getElementValues()
                        .values().
                        toArray();
                if (values.length > 1) {
                    var value = values[0].toString().replaceAll("\"", "");
                    var field = values[1].toString().replaceAll("\"", "");
                    list.add(output(value, field));
                } else if (values.length == 1) {
                    var value = values[0].toString().replaceAll("\"", "");
                    list.add(output(value));
                }
            }
        }

        return list;
    }
}
