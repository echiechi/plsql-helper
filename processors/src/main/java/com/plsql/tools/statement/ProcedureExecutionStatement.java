package com.plsql.tools.statement;

import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.templates.Templates;
import org.stringtemplate.v4.ST;

import java.util.*;
import java.util.stream.Collectors;

public class ProcedureExecutionStatement {
    private final String packageName;
    private final String procedureName;
    private final String procedureCallName;
    private final List<Parameter> parameters;

    public ProcedureExecutionStatement(String packageName, String procedureName, String procedureCallName) {
        this.packageName = packageName;
        this.procedureName = procedureName;
        this.procedureCallName = procedureCallName;
        this.parameters = new ArrayList<>();
    }

    public void withParameter(String parameterName) {
        withParameter(parameterName, ParameterType.IN);
    }

    public void withParameter(String parameterName, ParameterType type) {
        if (parameterName != null && !parameterName.trim().isEmpty()) {
            parameters.add(new Parameter(parameterName.trim(), type));
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
        ST templateBuilder = new ST(Templates.PROCEDURE_CALL_TEMPLATE);

        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.name(),
                formatProcedureFullName());
        templateBuilder.add(TemplateParams.PROCEDURE_CALL_NAME.name(),
                getEffectiveProcedureCallName());
        templateBuilder.add(TemplateParams.PROCEDURE_PARAMETERS.name(),
                formatParameters());
        templateBuilder.add(TemplateParams.PACKAGE_CALL_NAME.name(),
                formatPackageCallName());

        return templateBuilder.render();
    }

    private String formatProcedureFullName() {
        return procedureName.replace(".", "_");
    }

    private String getEffectiveProcedureCallName() {
        return !procedureCallName.isEmpty() ? procedureCallName : procedureName;
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
        info.put("procedureCallName", procedureCallName);
        info.put("parameterCount", parameters.size());
        info.put("parameters", parameters.stream().map(Parameter::getName).collect(Collectors.toList()));
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcedureExecutionStatement that = (ProcedureExecutionStatement) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(procedureName, that.procedureName) &&
                Objects.equals(procedureCallName, that.procedureCallName) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, procedureName, procedureCallName, parameters);
    }

    @Override
    public String toString() {
        return String.format("ProcedureExecutionStatement{packageName='%s', procedureName='%s', procedureCallName='%s', parameters=%s }",
                packageName, procedureName, procedureCallName, parameters);
    }
}