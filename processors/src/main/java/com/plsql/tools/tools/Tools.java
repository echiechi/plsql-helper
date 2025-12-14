package com.plsql.tools.tools;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.enums.JdbcHelper;

import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Improved version of ProcessorTools with enhanced validation, null safety,
 * and better error handling.
 */
public class Tools {

    public static final String RETURN_NAME = "result";

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

    /**
     * Enhanced utility methods with null safety
     */
    public static boolean isGetter(String name) {
        return ValidationUtils.isValidGetter(name);
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
     * Utility method to safely filter elements by predicate
     */
    public static <T extends Element> List<T> filterElements(List<? extends Element> elements,
                                                             Class<T> elementType,
                                                             Predicate<T> predicate) {
        if (elements == null || elementType == null || predicate == null) {
            return new ArrayList<>();
        }

        return elements.stream()
                .filter(elementType::isInstance)
                .map(elementType::cast)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Safe string utility methods
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String defaultIfEmpty(String str, String defaultValue) {
        return isNullOrEmpty(str) ? defaultValue : str;
    }

    public static boolean isVoid(String returnType) {
        return "void".equals(returnType.trim());
    }

    public static String defaultInitTypeForList(JdbcHelper type) {
        if (!type.isCollection()) {
            throw new IllegalArgumentException("Provided type is not a collection");
        }
        if (type == JdbcHelper.LIST) {
            return "java.util.ArrayList<>()";
        } else if (type == JdbcHelper.SET) {
            return "java.util.HashSet<>()";
        }
        return null;
    }

    public static TypeElement getTypeElement(ProcessingContext context, Element element) {
        return getTypeElement(context, element.asType());
    }

    public static TypeElement getTypeElement(ProcessingContext context, TypeMirror type) {
        return (TypeElement) context.getProcessingEnv().getTypeUtils().asElement(type);
    }
}