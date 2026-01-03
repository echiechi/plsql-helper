package com.plsql.tools.tools;

import com.plsql.tools.annotations.PlsqlParam;
import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.info.ElementInfo;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Improved version of ProcessorTools with enhanced validation, null safety,
 * and better error handling.
 */
public class Tools {

    public static String extractName(ElementInfo elementInfo) {
        PlsqlParam param = elementInfo.getTypeInfo().getRawType().getAnnotation(PlsqlParam.class);
        if (param != null) {
            return param.value();
        } else {
            return elementInfo.getName();
        }
    }

    /**
     * Safely extracts package name from a TypeElement with validation
     */
    public static Optional<String> getPackageNameSafe(TypeElement clazz) {
        if (clazz == null) {
            return Optional.empty();
        }

        Element enclosingElement = clazz.getEnclosingElement();
        while (enclosingElement != null && enclosingElement.getKind() != ElementKind.PACKAGE) {
            enclosingElement = enclosingElement.getEnclosingElement();
        }

        if (enclosingElement instanceof PackageElement packageElement) {
            String packageName = packageElement.getQualifiedName().toString();
            return packageName.isEmpty() ? Optional.empty() : Optional.of(packageName);
        }

        return Optional.empty();
    }

    public static Optional<ExecutableElement> findMatchingMethod(String fieldName,
                                                                 List<ExecutableElement> methods,
                                                                 Predicate<ExecutableElement> check) {
        if (fieldName == null || fieldName.isEmpty()) {
            return Optional.empty();
        }
        return methods.stream()
                .filter(check)
                .map(ExecutableElement.class::cast)
                .findFirst();
    }

    public static boolean isModifierPresent(Element element, Modifier modifier) {
        return element != null &&
                modifier != null &&
                element.getModifiers().contains(modifier);
    }

    public static String extractNameAsStr(Element element) {
        if (element.getSimpleName() == null) {
            return "";
        }
        return element.getSimpleName().toString();
    }

    /**
     * Safe string utility methods
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isVoid(String returnType) {
        return "void".equals(returnType.trim());
    }

    public static String defaultInitTypeForList(TypeMirror typeMirror) {
        var type = TypeMapper.fromSimpleName(typeMirror.toString());
        if (type == null) {
            throw new IllegalArgumentException("Type is not supported");
        }
        if (!type.isCollection()) {
            throw new IllegalArgumentException("Provided type is not a collection");
        }
        if (type == TypeMapper.LIST) {
            return java.util.ArrayList.class.getCanonicalName();
        } else if (type == TypeMapper.SET) {
            return java.util.HashSet.class.getCanonicalName();
        } else {
            throw new IllegalArgumentException("Type is not supported yet " + type);
        }
    }
}