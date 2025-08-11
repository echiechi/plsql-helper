package com.plsql.tools;

import com.plsql.tools.tools.MessageUtils;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Collection;

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
        messageUtils.logWarning(message);
    }
    public void logWarnings(Collection<String> messages) {
        messages.forEach(messageUtils::logWarning);
    }
    public ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }
}
