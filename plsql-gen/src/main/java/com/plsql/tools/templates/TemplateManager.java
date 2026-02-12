package com.plsql.tools.templates;

import java.util.Map;

@FunctionalInterface
public interface TemplateManager<T extends Enum<T>> {
    String render(String templateName, Map<T, String> parameters);
}
