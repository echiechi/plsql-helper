package com.plsql.tools.tools;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ElementFinder {
    private final Map<String, TypeElement> elementCache = new HashMap<>();
    private final RoundEnvironment roundEnv;

    public ElementFinder(RoundEnvironment roundEnv) {
        this.roundEnv = Objects.requireNonNull(roundEnv, "RoundEnvironment cannot be null");
    }

    public Optional<TypeElement> findElementByName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(elementCache.computeIfAbsent(className, this::searchForElement));
    }

    private TypeElement searchForElement(String className) {
        return roundEnv.getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> className.equals(element.getQualifiedName().toString()))
                .findFirst()
                .orElse(null);
    }

    public void clearCache() {
        elementCache.clear();
    }
}
