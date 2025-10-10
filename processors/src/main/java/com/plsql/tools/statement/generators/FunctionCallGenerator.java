package com.plsql.tools.statement.generators;

import com.plsql.tools.statement.Parameter;
import com.plsql.tools.statement.ParameterType;
import com.plsql.tools.templates.FunctionCallParams;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionCallGenerator { // TODO : can add interface for procedure/function call
    public static final String FUNCTION_CALL_TEMPLATE = """
            public static final String <FUNCTION_FULL_NAME> = "{ ? = call <PACKAGE_CALL_NAME><FUNCTION_CALL_NAME>(<FUNCTION_PARAMETERS>) }";
            """;
    private final String packageName;
    private final String functionName;
    private final List<Parameter> parameters;

    public FunctionCallGenerator(String packageName, String functionName) {
        this.packageName = packageName;
        this.functionName = functionName;
        this.parameters = new ArrayList<>();
    }

    public void withParameter(String parameterName) {
        withParameter(parameterName, ParameterType.IN);
    }

    private void withParameter(String parameterName, ParameterType type) {
        if (parameterName != null && !parameterName.trim().isEmpty()) {
            parameters.add(new Parameter(CaseConverter.toSnakeCase(parameterName.trim()), type));
        }
    }

    /**
     * Builds the SQL call statement
     */
    public String build() {
        return buildWithTemplate();
    }

    /**
     * Builds the statement using StringTemplate
     */
    private String buildWithTemplate() {
        ST templateBuilder = new ST(FUNCTION_CALL_TEMPLATE);

        templateBuilder.add(FunctionCallParams.FUNCTION_FULL_NAME.name(),
                formatFunctionFullName());
        templateBuilder.add(FunctionCallParams.FUNCTION_CALL_NAME.name(), functionName);
        templateBuilder.add(FunctionCallParams.FUNCTION_PARAMETERS.name(),
                formatParameters());
        templateBuilder.add(FunctionCallParams.PACKAGE_CALL_NAME.name(),
                formatFunctionCallName());

        return templateBuilder.render();
    }

    private String formatFunctionFullName() {
        return String.format("%s_%s", packageName, functionName);
    }

    private String formatParameters() {
        return parameters.stream()
                .map(Parameter::formatForCall)
                .collect(Collectors.joining(","));
    }

    private String formatFunctionCallName() {
        return packageName.isEmpty() ? "" : packageName + ".";
    }

    private Map<String, Object> generateBuildInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("packageName", packageName);
        info.put("procedureName", functionName);
        info.put("parameterCount", parameters.size());
        info.put("parameters", parameters.stream().map(Parameter::getName).collect(Collectors.toList()));
        return info;
    }
}
