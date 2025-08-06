package com.plsql.tools.tools.statement;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.JavaToSql;
import com.plsql.tools.mapping.ObjectField;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.plsql.tools.tools.fields.FieldMethodMapper.extractNameAsStr;

public class StatementGenerator {
    private final ProcessingContext processingContext;

    public StatementGenerator(ProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    public StatementGenerationResult generateSetStatements(Map<ObjectField, Element> fieldMethodMap) {
        if (fieldMethodMap == null || fieldMethodMap.isEmpty()) {
            return StatementGenerationResult.success(Collections.emptyList());
        }

        List<String> statements = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        fieldMethodMap.forEach((field, method) -> {
            try {
                String statement = generateStatementForField(field, method, warnings);
                if (statement != null) {
                    statements.add(statement);
                }
            } catch (Exception e) {
                String error = "Failed to generate statement for field: " +
                        extractNameAsStr(field.getField()) + " - " + e.getMessage();
                errors.add(error);
                processingContext.logError(error);
            }
        });

        warnings.forEach(processingContext::logWarning);

        return errors.isEmpty()
                ? StatementGenerationResult.success(statements, warnings)
                : StatementGenerationResult.failure(errors);
    }

    private String generateStatementForField(ObjectField field, Element method, List<String> warnings) {
        if (field.getField() == null) {
            warnings.add("Null field encountered, skipping");
            return null;
        }

        String fieldTypeStr = field.getField().asType().toString();
        JavaToSql type = JavaToSql.fromSimpleName(fieldTypeStr);

        if (type == null) {
            throw new IllegalStateException("Unmapped java type: " + fieldTypeStr);
        }

        String setter = type.getJdbcSetterMethod();
        String objectName = field.getObjectName();

        if (objectName == null || objectName.isEmpty()) {
            return String.format("stmt.%s(pos++, %s);", setter, field.getField().getSimpleName());
        }

        String getter = generateGetter(field, method);

        if (type.isDateTime()) {
            return String.format("stmt.%s(pos++, %s);", setter, handleDateWithNullCheck(getter, type));
        } else if (type == JavaToSql.CHARACTER) {
            return String.format("stmt.%s(pos++, String.valueOf(%s));", setter, getter);
        } else {
            return String.format("stmt.%s(pos++, %s);", setter, getter);
        }
    }

    private String generateGetter(ObjectField field, Element method) {
        String objectName = field.getObjectName();

        if (method != null) {
            return String.format("%s.%s()", objectName, method.getSimpleName().toString());
        } else {
            return String.format("%s.%s", objectName, field.getField().getSimpleName().toString());
        }
    }

    private String handleDateWithNullCheck(String dateObjGetter, JavaToSql type) {
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
