package com.plsql.tools.tools;

import com.plsql.tools.ProcessingContext;
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
import java.util.Objects;

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
        if (variableInfo instanceof ObjectInfo objectInfo) {
            outputs.addAll(objectInfo.getFieldInfoSet()
                    .stream()
                    .map(k -> new OutputElement(k.getField().getAnnotation(Output.class), k.getField()))
                    .peek(o -> o.isFieldOutput = true)
                    .peek(o -> {
                        var typeElement = Tools.getTypeElement(context, o.element);
                        if (isWrapped(typeElement)) {
                            o.wrappedType = extractDeclaredType(o.element.asType());
                        }
                    })
                    .filter(o -> Objects.nonNull(o.output))
                    .toList());
            if (outputs.isEmpty() && objectInfo.getField().getAnnotation(Output.class) != null) {
                outputs.add(new OutputElement(objectInfo.getField().getAnnotation(Output.class), objectInfo.getField()));
            } else if (objectInfo.getOutput() != null) {
                outputs.add(new OutputElement(objectInfo.getOutput(), objectInfo.getField()));
            } else if (outputs.isEmpty()) {
                outputs.add(new OutputElement(defaultOutput(), objectInfo.getField()));
            }
        } else {
            outputs.add(new OutputElement(variableInfo.getOutput(), variableInfo.getField()));
        }
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

    public Output defaultOutput() {
        return new Output() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Output.class;
            }

            @Override
            public String value() {
                return "ps_curs";
            }
        };
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
        };
    }
}
