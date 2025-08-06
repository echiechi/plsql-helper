package com.plsql.tools.sql;

import com.plsql.tools.TemplateParams;
import com.plsql.tools.common.ValidationResult;
import com.plsql.tools.utils.PlsqlNameValidator;
import org.stringtemplate.v4.ST;

import java.util.*;
import java.util.stream.Collectors;

public class ProcedureExecutionStatement {
    public static final String PROCEDURE_CALL_TEMPLATE = """
            public static final String <PROCEDURE_FULL_NAME> = "{ call <PACKAGE_CALL_NAME><PROCEDURE_CALL_NAME>(<PROCEDURE_PARAMETERS>) }";
            """;

    private final String packageName;
    private final String procedureName;
    private final String procedureCallName;
    private final List<Parameter> parameters;
    private final boolean validated;

    private ProcedureExecutionStatement(ProcedureExecutionStatement.Builder builder) {
        this.packageName = builder.packageName;
        this.procedureName = builder.procedureName;
        this.procedureCallName = builder.procedureCallName;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(builder.parameters));
        this.validated = builder.validated;
    }

    /**
     * Creates a new builder for ProcedureExecutionStatement
     */
    public static ProcedureExecutionStatement.Builder builder() {
        return new ProcedureExecutionStatement.Builder();
    }

    /**
     * Creates a builder with procedure name (for backward compatibility)
     */
    public static ProcedureExecutionStatement.Builder builder(String procedureName) {
        return new ProcedureExecutionStatement.Builder().withProcedureName(procedureName);
    }

    /**
     * Creates a builder with package, procedure, and call names (for backward compatibility)
     */
    public static ProcedureExecutionStatement.Builder builder(String packageName, String procedureName, String procedureCallName) {
        return new ProcedureExecutionStatement.Builder()
                .withPackageName(packageName)
                .withProcedureName(procedureName)
                .withProcedureCallName(procedureCallName);
    }

    /**
     * Builder class for fluent API construction
     */
    public static class Builder {
        private String packageName = "";
        private String procedureName;
        private String procedureCallName = "";
        private final List<Parameter> parameters = new ArrayList<>();
        private boolean validated = false;
        private boolean strictValidation = true;

        private Builder() {
        }

        public ProcedureExecutionStatement.Builder withPackageName(String packageName) {
            this.packageName = packageName != null ? packageName.trim() : "";
            return this;
        }

        public ProcedureExecutionStatement.Builder withProcedureName(String procedureName) {
            this.procedureName = procedureName != null ? procedureName.trim() : null;
            return this;
        }

        public ProcedureExecutionStatement.Builder withProcedureCallName(String procedureCallName) {
            this.procedureCallName = procedureCallName != null ? procedureCallName.trim() : "";
            return this;
        }

        public ProcedureExecutionStatement.Builder withParameter(String parameterName) {
            return withParameter(parameterName, ParameterType.IN);
        }

        public ProcedureExecutionStatement.Builder withParameter(String parameterName, ParameterType type) {
            if (parameterName != null && !parameterName.trim().isEmpty()) {
                parameters.add(new Parameter(parameterName.trim(), type));
            }
            return this;
        }

        public ProcedureExecutionStatement.Builder addParameters(Collection<String> parameterNames) {
            if (parameterNames != null) {
                parameterNames.stream()
                        .filter(name -> name != null && !name.trim().isEmpty())
                        .forEach(name -> parameters.add(new Parameter(name.trim(), ParameterType.IN)));
            }
            return this;
        }

        public ProcedureExecutionStatement.Builder clearParameters() {
            parameters.clear();
            return this;
        }

        public ProcedureExecutionStatement.Builder withStrictValidation(boolean strictValidation) {
            this.strictValidation = strictValidation;
            return this;
        }

        public void validate() {
            ValidationResult validationResult = performValidation();

            if (!validationResult.isValid() && strictValidation) {
                throw new IllegalArgumentException("Validation failed: " +
                        String.join(", ", validationResult.getErrors()));
            }
            this.validated = true;
        }

        private ValidationResult performValidation() {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Validate procedure name
            if (procedureName == null || procedureName.trim().isEmpty()) {
                errors.add("Procedure name cannot be null or empty");
            } else if (!PlsqlNameValidator.isValidPlsqlName(procedureName)) {
                errors.add("Invalid procedure name: " + procedureName);
            }

            // Validate package name if provided
            if (!packageName.isEmpty() && !PlsqlNameValidator.isValidPlsqlName(packageName)) {
                errors.add("Invalid package name: " + packageName);
            }

            // Validate procedure call name if provided
            if (!procedureCallName.isEmpty() && !PlsqlNameValidator.isValidPlsqlName(procedureCallName)) {
                errors.add("Invalid procedure call name: " + procedureCallName);
            }

            // Validate parameters
            Set<String> duplicateChecker = new HashSet<>();
            for (Parameter param : parameters) {
                if (!PlsqlNameValidator.isValidPlsqlName(param.getName())) {
                    errors.add("Invalid parameter name: " + param.getName());
                }

                if (!duplicateChecker.add(param.getName().toLowerCase())) {
                    warnings.add("Duplicate parameter name detected: " + param.getName());
                }
            }

            return new ValidationResult(errors, warnings);
        }

        public ProcedureExecutionStatement build() {
            if (!validated && strictValidation) {
                validate(); // Auto-validate if not already done
            }

            return new ProcedureExecutionStatement(this);
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

    /**
     * Enhanced build method with detailed result information
     */
    public StatementBuildResult buildWithDetails() {
        try {
            String statement = build();
            return StatementBuildResult.success(statement, generateBuildInfo());
        } catch (Exception e) {
            return StatementBuildResult.failure("Failed to build statement: " + e.getMessage());
        }
    }

    private Map<String, Object> generateBuildInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("packageName", packageName);
        info.put("procedureName", procedureName);
        info.put("procedureCallName", procedureCallName);
        info.put("parameterCount", parameters.size());
        info.put("parameters", parameters.stream().map(Parameter::getName).collect(Collectors.toList()));
        info.put("validated", validated);
        return info;
    }

    // Getters for immutable access
    public String getPackageName() {
        return packageName;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public String getProcedureCallName() {
        return procedureCallName;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public boolean isValidated() {
        return validated;
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
        return String.format("ProcedureExecutionStatement{packageName='%s', procedureName='%s', procedureCallName='%s', parameters=%s, validated=%s}",
                packageName, procedureName, procedureCallName, parameters, validated);
    }
}