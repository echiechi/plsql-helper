package com.plsql.tools;

import com.plsql.tools.tools.MessageUtils;
import com.plsql.tools.tools.extraction.cache.ExtractionCache;
import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessingContext {
    private final ProcessingEnvironment processingEnv;
    private final MessageUtils messageUtils;
    private boolean isDebugEnabled;

    private final SimpleCache<TypeMirror, List<AttachedElementInfo>> cache = new ExtractionCache();

    public ProcessingContext(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.messageUtils = new MessageUtils(processingEnv);
    }

    public ProcessingContext(ProcessingEnvironment processingEnv, boolean isDebugEnabled) {
        this.processingEnv = processingEnv;
        this.messageUtils = new MessageUtils(processingEnv);
        this.isDebugEnabled = isDebugEnabled;
    }

    public void logInfo(Object... messages) {
        messageUtils.logInfo(Arrays.stream(messages)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
    }

    public void logInfoDeco(Object... messages) {
        logInfoDeco(Arrays.stream(messages)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
    }

    public void logInfo(String message) {
        messageUtils.logInfo(message);
    }

    public void logInfoDeco(String message) {
        if (message != null) {
            messageUtils.logInfo("-".repeat(message.length()));
            messageUtils.logInfo(message);
            messageUtils.logInfo("-".repeat(message.length()));
        }
    }

    public void logError(Object... messages) {
        messageUtils.logError(Arrays.stream(messages)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
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

    public void logDebug(String message) {
        if (isDebugEnabled) {
            messageUtils.logDebug(message);
        }
    }

    public void logDebug(Object... messages) {
        logDebug(Arrays.stream(messages)
                .map(String::valueOf)
                .collect(Collectors.joining(" ")));
    }

    public ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }

    public SimpleCache<TypeMirror, List<AttachedElementInfo>> getCache() {
        return cache;
    }
}
