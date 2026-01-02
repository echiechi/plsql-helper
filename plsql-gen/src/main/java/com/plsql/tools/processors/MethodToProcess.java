package com.plsql.tools.processors;

import javax.lang.model.element.ExecutableElement;

public record MethodToProcess(ExecutableElement method, String suffix) {
}
