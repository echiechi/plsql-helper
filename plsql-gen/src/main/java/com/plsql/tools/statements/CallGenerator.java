package com.plsql.tools.statements;

import com.plsql.tools.statements.params.Parameter;
import com.plsql.tools.statements.params.ParameterType;
import com.plsql.tools.utils.CaseConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CallGenerator implements Generator {
    protected final String packageName;
    protected final String name;
    protected final List<Parameter> parameters;
    protected String suffix;

    public CallGenerator(String packageName, String name) {
        this.packageName = packageName;
        this.name = name;
        this.parameters = new ArrayList<>();
        this.suffix = "";
    }

    public void withSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void withParameter(String parameterName) {
        withParameter(parameterName, ParameterType.IN);
    }

    public String generate() {
        return buildWithTemplate();
    }

    public abstract String buildWithTemplate();

    protected String formatFullName() {
        if (packageName == null || packageName.isBlank()) {
            return name;
        }
        return String.format("%s_%s", packageName, name);
    }

    protected String formatParameters() {
        return parameters.stream()
                .map(Parameter::formatForCall)
                .collect(Collectors.joining(","));
    }

    protected String formatCallName() {
        return packageName.isEmpty() ? "" : packageName + ".";
    }

    private void withParameter(String parameterName, ParameterType type) {
        if (parameterName != null && !parameterName.trim().isEmpty()) {
            parameters.add(new Parameter(CaseConverter.toSnakeCase(parameterName.trim()), type));
        }
    }
}
