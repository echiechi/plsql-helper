package com.plsql.tools.tools;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Objects;

public class MessageUtils {
    private final ProcessingEnvironment processingEnv;

    public MessageUtils(ProcessingEnvironment processingEnv) {
        this.processingEnv = Objects.requireNonNull(processingEnv, "ProcessingEnvironment cannot be null");
    }

    public void logInfo(String message) {
        if (message != null && !message.trim().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
        }
    }

    public void logWarning(String message) {
        if (message != null && !message.trim().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
        }
    }

    public void logError(String message) {
        if (message != null && !message.trim().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
        }
    }

    public void logError(String message, Element element) {
        if (message != null && !message.trim().isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
        }
    }
}
