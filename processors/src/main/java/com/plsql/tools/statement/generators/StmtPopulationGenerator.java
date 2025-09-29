package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.ObjectInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StmtPopulationGenerator {

    private final ProcessingContext context;

    public StmtPopulationGenerator(ProcessingContext context) {
        this.context = context;
    }

    public List<String> generateStatements(List<VariableInfo> variableInfoList) {
        List<String> statements = new ArrayList<>();
        variableInfoList.forEach(v -> {
            if (v.isSimple()) {
                statements.add(simpleFieldStatement(v));
            } else {
                statements.addAll(handleObjectInfo((ObjectInfo) v));
            }
        });
        return statements;
    }

    private String simpleFieldStatement(VariableInfo variableInfo) {
        String variableName = variableInfo.getName();
        if (variableInfo.getJdbcMappedType().isDateTime()) {
            variableName = handleDateWithNullCheck(variableInfo.getName(), variableInfo.getJdbcMappedType());
        } else if (variableInfo.getJdbcMappedType() == JdbcHelper.CHARACTER) {
            variableName = String.format("String.valueOf(%s)", variableInfo.getName());
        }
        return String.format("stmt.%s(pos++, %s);", variableInfo.getJdbcMappedType().getJdbcSetterMethod(),
                variableName);
    }

    private List<String> handleObjectInfo(ObjectInfo objectInfo) {
        List<String> statements = new ArrayList<>();
        for (var field : objectInfo.getFieldInfoSet()) {
            if (field.isSimple()) {
                statements.add(objectFieldStatement(objectInfo.getObjectName(), field));
            } else {
                Map<String, FieldInfo> getters = new LinkedHashMap<>();
                flattenObjectInfo(objectInfo, field, getters, "");
                getters.forEach((g, f) -> {
                    statements.add(objectFieldStatement(objectInfo.getObjectName() + "." + g, f));
                });
            }
        }
        return statements;
    }

    private String objectFieldStatement(String parentName, FieldInfo info) {
        var variableGetter = "";
        if (info.getGetter() != null) {
            variableGetter = String.format("%s.%s()", parentName, info.getGetter().getSimpleName().toString());
        } else {
            variableGetter = String.format("%s.%s", parentName, info.getName());
        }
        var finalVariableGetter = variableGetter;
        if (info.getJdbcMappedType().isDateTime()) {
            finalVariableGetter = handleDateWithNullCheck(variableGetter, info.getJdbcMappedType());
        } else if (info.getJdbcMappedType() == JdbcHelper.CHARACTER) {
            finalVariableGetter = String.format("String.valueOf(%s)", variableGetter);
        }
        return String.format("stmt.%s(pos++, %s);", info.getJdbcMappedType().getJdbcSetterMethod(),
                finalVariableGetter);
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

    private void flattenObjectInfo(ObjectInfo objectInfo, FieldInfo fieldInfo, Map<String, FieldInfo> list, String currentGetter) {
        var getter = fieldInfo.getGetter().toString();
        TypeElement typeElement = Tools.getTypeElement(context, fieldInfo.getField());
        var fieldInfoSet = objectInfo.getNestedObjects().get(typeElement);
        currentGetter += getter + ".";
        if (fieldInfoSet != null) {
            for (var field : fieldInfoSet) {
                if (field.isSimple()) {
                    list.put(currentGetter.substring(0, currentGetter.length() - 1), field);
                } else {
                    flattenObjectInfo(objectInfo, field, list, currentGetter);
                }
            }
        }
    }

}
