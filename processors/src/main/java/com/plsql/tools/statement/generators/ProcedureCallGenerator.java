package com.plsql.tools.statement.generators;

import com.plsql.tools.statement.Parameter;
import com.plsql.tools.statement.ParameterType;
import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import java.util.*;
import java.util.stream.Collectors;

public class ProcedureCallGenerator {
    public static final String PROCEDURE_CALL_TEMPLATE = """
            public static final String <PROCEDURE_FULL_NAME> = "{ call <PACKAGE_CALL_NAME><PROCEDURE_CALL_NAME>(<PROCEDURE_PARAMETERS>) }";
            """;
    private final String packageName;
    private final String procedureName;
    private final List<Parameter> parameters;

    public ProcedureCallGenerator(String packageName, String procedureName) {
        this.packageName = packageName;
        this.procedureName = procedureName;
        this.parameters = new ArrayList<>();
    }

    public void withParameter(String parameterName) {
        withParameter(parameterName, ParameterType.IN);
    }

    public void withParameter(String parameterName, ParameterType type) {
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
        ST templateBuilder = new ST(PROCEDURE_CALL_TEMPLATE);

        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(),
                formatProcedureFullName());
        templateBuilder.add(TemplateParams.PROCEDURE_CALL_NAME.name(), procedureName);
        templateBuilder.add(TemplateParams.PROCEDURE_PARAMETERS.name(),
                formatParameters());
        templateBuilder.add(TemplateParams.PACKAGE_CALL_NAME.name(),
                formatPackageCallName());

        return templateBuilder.render();
    }

    private String formatProcedureFullName() {
        return String.format("%s_%s", packageName, procedureName);
    }

    private String formatParameters() {
        return parameters.stream()
                .map(Parameter::formatForCall)
                .collect(Collectors.joining(","));
    }

    private String formatPackageCallName() {
        return packageName.isEmpty() ? "" : packageName + ".";
    }

    private Map<String, Object> generateBuildInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("packageName", packageName);
        info.put("procedureName", procedureName);
        info.put("parameterCount", parameters.size());
        info.put("parameters", parameters.stream().map(Parameter::getName).collect(Collectors.toList()));
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcedureCallGenerator that = (ProcedureCallGenerator) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(procedureName, that.procedureName) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, procedureName, parameters);
    }

    @Override
    public String toString() {
        return String.format("ProcedureCallGenerator{packageName='%s', procedureName='%s', parameters=%s }",
                packageName, procedureName, parameters);
    }
}