package com.plsql.tools;

import com.plsql.tools.tools.MessageUtils;

import javax.annotation.processing.ProcessingEnvironment;

public class ProcessingContext {
    private final ProcessingEnvironment processingEnv;
    private final MessageUtils messageUtils;

    public ProcessingContext(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.messageUtils = new MessageUtils(processingEnv);
    }

    public void logInfo(String message) {
        messageUtils.logInfo(message);
    }

    public void logError(String message) {
        messageUtils.logError(message);
    }

    public void logWarning(String message) {
        processingEnv.getMessager().printMessage(
                javax.tools.Diagnostic.Kind.WARNING, message
        );
    }

    public ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }
}
