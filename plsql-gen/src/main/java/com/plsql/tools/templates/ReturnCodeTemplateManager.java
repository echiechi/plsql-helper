package com.plsql.tools.templates;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.Map;

public class ReturnCodeTemplateManager {
    public static String PROCESS_RESULT_SET = "processResultSet";

    public static String PROCESS_SIMPLE_RESULT_SET = "processSimpleResultSet";

    public static String PROCESS_OPTIONAL_RESULT_SET = "processOptionalResultSet";

    private final STGroup codeSnippets;

    public ReturnCodeTemplateManager() {
        this.codeSnippets = new STGroupFile("templates/codeSnippets.stg");
    }

    public String renderProcessResultSet(Map<CodeSnippets.ResultSetParams, String> codeSnippetsContext) {
        ST template = codeSnippets.getInstanceOf(PROCESS_RESULT_SET);
        codeSnippetsContext.forEach((k, v) -> {
            template.add(k.name(), v);
        });
        return template.render();
    }

    public String renderSimpleProcessResultSet(Map<CodeSnippets.SimpleResultSetParams, String> codeSnippetsContext) {
        ST template = codeSnippets.getInstanceOf(PROCESS_SIMPLE_RESULT_SET);
        codeSnippetsContext.forEach((k, v) -> {
            template.add(k.name(), v);
        });
        return template.render();
    }

    public String renderOptionalProcessResultSet(Map<CodeSnippets.OptionalResultSetParams, String> codeSnippetsContext) {
        ST template = codeSnippets.getInstanceOf(PROCESS_OPTIONAL_RESULT_SET);
        codeSnippetsContext.forEach((k, v) -> {
            template.add(k.name(), v);
        });
        return template.render();
    }


}
