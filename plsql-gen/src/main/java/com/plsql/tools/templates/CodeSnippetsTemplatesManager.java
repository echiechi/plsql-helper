package com.plsql.tools.templates;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.Map;

public class CodeSnippetsTemplatesManager<T extends Enum<T>> implements TemplateManager<T> {
    public static String PROCESS_RESULT_SET = "processResultSet";

    public static String PROCESS_SIMPLE_RESULT_SET = "processSimpleResultSet";

    public static String PROCESS_OPTIONAL_RESULT_SET = "processOptionalResultSet";

    public static String METHOD_TEMPLATE = "methodTemplate";

    public static String PROCEDURE_METHOD_TEMPLATE = "procedureMethodTemplate";
    public static String FUNCTION_METHOD_TEMPLATE = "functionMethodTemplate";
    private final STGroup codeSnippets;

    public CodeSnippetsTemplatesManager() {
        this.codeSnippets = new STGroupFile("templates/codeSnippets.stg");
    }

    @Override
    public String render(String templateName, Map<T, String> parameters) {
        ST template = codeSnippets.getInstanceOf(templateName);
        parameters.forEach((k, v) -> {
            template.add(k.name(), v);
        });
        return template.render();
    }
}
