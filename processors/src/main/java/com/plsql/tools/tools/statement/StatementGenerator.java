package com.plsql.tools.tools.statement;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.templates.ResultSetTemplateParams;
import com.plsql.tools.templates.Templates;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.fields.ExtractedField;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.sql.JDBCType;
import java.util.*;

import static com.plsql.tools.tools.fields.FieldMethodMapper.extractNameAsStr;

// TODO: decompose to multiple classes
public class StatementGenerator {
    private final ProcessingContext processingContext;

    public StatementGenerator(ProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    public StatementGenerationResult generateSetStatements(Set<ExtractedField> parameterSet) {
        if (parameterSet == null || parameterSet.isEmpty()) {
            return StatementGenerationResult.success(Collections.emptyList());
        }

        List<String> statements = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        parameterSet.forEach((parameter) -> {
            try {
                String statement = generateStatementForField(parameter, warnings);
                if (statement != null) {
                    statements.add(statement);
                }
            } catch (Exception e) {
                String error = "Failed to generate statement for field: " +
                        extractNameAsStr(parameter.getParameter()) + " - " + e.getMessage();
                errors.add(error);
                processingContext.logError(error);
            }
        });

        warnings.forEach(processingContext::logWarning);

        return errors.isEmpty()
                ? StatementGenerationResult.success(statements, warnings)
                : StatementGenerationResult.failure(errors);
    }

    public StatementGenerationResult generateGetStatements(Output[] outputs) {
        List<String> statements = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            String pos = "pos++";
            int i = 0;
            for (Output output : outputs) {
                if (++i == outputs.length) {
                    pos = "pos";
                }
                var type = JdbcHelper.fromSimpleName(Tools.getType(output));
                if (type != null) {
                    statements.add(String.format("var return%d = (%s)stmt.%s(%s);",
                            i, Tools.getType(output), type.getJdbcGetterMethod(), pos));
                }
            }
        } catch (Exception e) {
            String error = "Failed to generate statement for outputs: " + Arrays.toString(outputs);
            errors.add(error);
            processingContext.logError(error);
        }
        return errors.isEmpty()
                ? StatementGenerationResult.success(statements, Collections.emptyList())
                : StatementGenerationResult.failure(errors);
    }

    public StatementGenerationResult generateResultSetExtraction(Output[] outputs, Map<DeclaredType, Set<ExtractedField>> extractedReturnFields) {
        List<String> statements = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 1; i < outputs.length + 1; i++) {
            ST template = new ST(Templates.RESULT_SET_TEMPLATE);
            for (var entry : extractedReturnFields.entrySet()) {
                var declaredType = JdbcHelper.fromSimpleName(entry.getKey().asElement().toString());
                if (declaredType != null && declaredType.isCollection()) {
                    var listInitStatement = String.format("%s toReturn = new %s;", entry.getKey().toString(), Tools.defaultInitTypeForList(declaredType));
                    template.add(ResultSetTemplateParams.INIT_LIST_STATEMENT.name(), listInitStatement);
                    template.add(ResultSetTemplateParams.ADD_TO_LIST.name(), "toReturn.add(obj);");
                } else {
                    template.add(ResultSetTemplateParams.INIT_LIST_STATEMENT.name(), "");
                }
                List<? extends TypeMirror> arguments = entry.getKey().getTypeArguments();
                if (arguments != null && !arguments.isEmpty()) {
                    template.add(ResultSetTemplateParams.OBJECT_INIT_STATEMENT.name(),
                            arguments.getFirst() + " obj = new " + arguments.getFirst() + "();");
                }
                List<String> setterStatements = new ArrayList<>();
                for (var field : entry.getValue()) {
                    var setter = field.getSetter().getSimpleName().toString();
                    var columnName = CaseConverter.toSnakeCase(field.getParameter().getSimpleName().toString());
                    var fieldType = JdbcHelper.fromSimpleName(field.getParameter().asType().toString());
                    if (fieldType == null) {
                        errors.add("can't process nested objects for now");
                    } else {
                        if (fieldType.isDateTime()) {
                            String casting = switch (fieldType) {
                                case LOCAL_DATE -> "toLocalDate()";
                                case LOCAL_DATE_TIME -> "toLocalDateTime()";
                                case LOCAL_TIME -> "toLocalTime()";
                                default -> null;
                            };
                            if (casting != null) {
                                setterStatements.add(String.format("obj.%s(rs.%s(\"%s\") != null ? rs.%s(\"%s\").%s : null);",
                                        setter,
                                        fieldType.getJdbcGetterMethod(),
                                        columnName,
                                        fieldType.getJdbcGetterMethod(),
                                        columnName,
                                        casting
                                ));
                            } else {
                                setterStatements.add(
                                        String.format("obj.%s(rs.%s(\"%s\"));", setter, fieldType.getJdbcGetterMethod(), columnName));
                            }
                        } else {
                            setterStatements.add(
                                    String.format("obj.%s(rs.%s(\"%s\"));", setter, fieldType.getJdbcGetterMethod(), columnName));
                        }
                    }
                }
                template.add(ResultSetTemplateParams.SETTER_STATEMENTS.name(), setterStatements);
                template.add(ResultSetTemplateParams.POSITION.name(), "pos - " + outputs.length + " + " + i);
                statements.add(template.render());
                processingContext.logInfo(template.render());
            }
        }
        return errors.isEmpty()
                ? StatementGenerationResult.success(statements, Collections.emptyList())
                : StatementGenerationResult.failure(errors);
    }

    public StatementGenerationResult generateOutStatements(Output[] outputs) {
        List<String> statements = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            String pos = "pos++";
            int i = 0;
            for (Output output : outputs) {
                processingContext.logInfo(output.name());
                if (++i == outputs.length) {
                    pos = "pos";
                }
                var type = JdbcHelper.fromSimpleName(Tools.getType(output));
                statements.add(String.format("stmt.registerOutParameter(%s, JDBCType.%s);", pos, (type == null) ?
                        JDBCType.REF_CURSOR.name() : type.getJdbcType().name()));
            }
        } catch (Exception e) {
            String error = "Failed to generate statement registration for outputs: " + Arrays.toString(outputs);
            errors.add(error);
            processingContext.logError(error);
            processingContext.logError(e.getMessage());
        }
        return errors.isEmpty()
                ? StatementGenerationResult.success(statements, Collections.emptyList())
                : StatementGenerationResult.failure(errors);
    }

    private String generateStatementForField(ExtractedField field, List<String> warnings) {
        if (field.getParameter() == null) {
            warnings.add("Null field encountered, skipping");
            return null;
        }

        String fieldTypeStr = field.getParameter().asType().toString();
        JdbcHelper type = JdbcHelper.fromSimpleName(fieldTypeStr);

        if (type == null) {
            throw new IllegalStateException("Unmapped java type: " + fieldTypeStr);
        }

        String setter = type.getJdbcSetterMethod();
        String objectName = field.getParentObjectName();

        if (objectName == null || objectName.isEmpty()) {
            return String.format("stmt.%s(pos++, %s);", setter, field.getParameter().getSimpleName());
        }

        String getter = generateGetter(field);

        if (type.isDateTime()) {
            return String.format("stmt.%s(pos++, %s);", setter, handleDateWithNullCheck(getter, type));
        } else if (type == JdbcHelper.CHARACTER) {
            return String.format("stmt.%s(pos++, String.valueOf(%s));", setter, getter);
        } else {
            return String.format("stmt.%s(pos++, %s);", setter, getter);
        }
    }

    private String generateGetter(ExtractedField extractedField) {
        String objectName = extractedField.getParentObjectName();

        if (extractedField.getGetter() != null) {
            return String.format("%s.%s()", objectName, extractedField.getGetter().getSimpleName().toString());
        } else {
            return String.format("%s.%s", objectName, extractedField.getParameter().getSimpleName().toString());
        }
    }

    private String handleDateWithNullCheck(String dateObjGetter, JdbcHelper type) {
        return switch (type) {
            case DATE -> String.format(
                    "%s == null ? null : java.sql.Date.valueOf(%s.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())",
                    dateObjGetter, dateObjGetter);
            case LOCAL_DATE -> String.format("%s == null ? null : java.sql.Date.valueOf(%s)",
                    dateObjGetter, dateObjGetter);
            case LOCAL_DATE_TIME -> String.format("%s == null ? null : java.sql.Timestamp.valueOf(%s)",
                    dateObjGetter, dateObjGetter);
            case LOCAL_TIME -> String.format("%s == null ? null : java.sql.Time.valueOf(%s)",
                    dateObjGetter, dateObjGetter);
            default -> dateObjGetter;
        };
    }
}
