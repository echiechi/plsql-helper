package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.enums.CallableType;
import com.plsql.tools.tools.Tools;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

public class ExtractorValidator {
    public static void validateCallableMethod(ExecutableElement method) {
        validateMethod(method);

        PlsqlCallable annotation = method.getAnnotation(PlsqlCallable.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Method missing @PlsqlCallable annotation: " + method.getSimpleName()
            );
        }

        validateOutputAnnotations(method, annotation);
    }

    public static void validateMethod(ExecutableElement method) {
        if (method == null) {
            throw new IllegalArgumentException("Method cannot be null");
        }

        if (method.getKind() != ElementKind.METHOD) {
            throw new IllegalArgumentException(
                    "Element is not a method: " + method.getKind()
            );
        }
    }

    private static void validateOutputAnnotations(
            ExecutableElement method,
            PlsqlCallable annotation
    ) {
        TypeMirror returnType = method.getReturnType();
        Output[] outputs = annotation.outputs();

        boolean hasReturn = !Tools.isVoid(returnType.toString());
        boolean hasOutputs = outputs.length > 0;

        if (hasReturn && !hasOutputs) {
            throw new IllegalStateException(
                    "Method has return type but no @Output annotation: " +
                            method.getSimpleName()
            );
        }

        if (!hasReturn && hasOutputs) {
            throw new IllegalStateException(
                    "Method has @Output annotation but void return type: " +
                            method.getSimpleName()
            );
        }

        // Function-specific validation
        if (annotation.type() == CallableType.FUNCTION) {
            if (!hasReturn) {
                throw new IllegalStateException(
                        "Function cannot have void return type: " +
                                method.getSimpleName()
                );
            }
            if (outputs.length != 1) {
                throw new IllegalStateException(
                        "Function must have exactly one @Output, found: " +
                                outputs.length + " in " + method.getSimpleName()
                );
            }
        }
    }

}
