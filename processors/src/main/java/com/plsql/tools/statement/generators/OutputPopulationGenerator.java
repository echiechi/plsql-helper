package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.ElementTools;
import com.plsql.tools.tools.fields.FieldMethodExtractor;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.ObjectInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.plsql.tools.enums.JdbcHelper.CHARACTER;
import static com.plsql.tools.enums.JdbcHelper.CHARACTER_WRAPPER;
import static com.plsql.tools.templates.ResultSetTemplateParams.*;
import static com.plsql.tools.tools.ElementTools.OutputElement;
import static com.plsql.tools.tools.Tools.getTypeElement;

public class OutputPopulationGenerator {

    public static final String ONE_OUTPUT_TEMPLATE = """
            <OBJECT_INIT_STATEMENT>
            try(<STMT_RESULT_TYPE> rs = (<STMT_RESULT_TYPE>)stmt.<STMT_GETTER>(<POSITION>);){
                if (!rs.next()) {
                            <HANDLE_EMPTY_STATEMENT>
                    } else {
                        do {
                            <SETTER_STATEMENTS:{statement | <statement>
                            }>
                        } while (rs.next());
                    }
            }""";

    public static final String OPTIONAL_OUTPUT_TEMPLATE = """
            <OBJECT_INIT_STATEMENT>
            <WRAPPER_INIT_STATEMENT>
            try(<STMT_RESULT_TYPE> rs = (<STMT_RESULT_TYPE>)stmt.<STMT_GETTER>(<POSITION>);){
                if (!rs.next()) {
                            <HANDLE_EMPTY_STATEMENT>
                    } else {
                        do {
                            <SETTER_STATEMENTS:{statement | <statement>
                            }>
                        } while (rs.next());
                    }
            }""";

    public static final String LIST_OUTPUT_TEMPLATE = """
            <WRAPPER_INIT_STATEMENT>
            try(<STMT_RESULT_TYPE> rs = (<STMT_RESULT_TYPE>)stmt.<STMT_GETTER>(<POSITION>);){
                if (!rs.next()) {
                            <HANDLE_EMPTY_STATEMENT>
                    } else {
                        do {
                            <OBJECT_INIT_STATEMENT>
                            <ADD_OBJECT_STATEMENT>
                            <SETTER_STATEMENTS:{statement | <statement>
                            }>
                        } while (rs.next());
                    }
            }""";
    public static final String SIMPLE_OUTPUT_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = stmt.<STMT_GETTER>(<POSITION>);
             """;
    public static final String OPTIONAL_SIMPLE_OUTPUT_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = java.util.Optional.of(stmt.<STMT_GETTER>(<POSITION>));
             """;
    private final ProcessingContext context;

    private final ElementTools elementTools;

    private final FieldMethodExtractor extractor;

    public OutputPopulationGenerator(ProcessingContext context) {
        this.context = context;
        this.elementTools = new ElementTools(context);
        this.extractor = FieldMethodExtractor.getInstance(context);
    }

    public List<String> generateStatements(VariableInfo variableInfo) {
        if (variableInfo == null) {
            return List.of();
        }
        List<String> statements = new ArrayList<>();
        if (variableInfo.isSimple()) {
            var output = new OutputElement(
                    variableInfo.getOutput(),
                    variableInfo.getField());
            output.pos = "pos";
            statements.add(handleOneVariableOutput(variableInfo, output));
        } else {
            var objectInfo = (ObjectInfo) variableInfo;
            statements.addAll(handleObjectInfo(objectInfo));
        }
        return statements;
    }

    private List<String> handleObjectInfo(ObjectInfo objectInfo) {
        List<OutputElement> outputs = elementTools.extractOutputs(objectInfo);
        if (outputs.size() == 1 && !outputs.getFirst().isFieldOutput) {
            OutputElement outputElement = outputs.getFirst();
            outputElement.pos = "pos";
            return List.of(handleOneObjectOutput(objectInfo, outputElement));
        } else {
            List<String> outputStatements = handleMultipleOutputs(objectInfo, outputs);
            outputStatements.add(initObject(objectInfo.getField(), objectInfo.getObjectName()));
            outputStatements.addAll(objectInfo.getFieldInfoSet()
                    .stream()
                    .filter(f -> f.getField().getAnnotation(Output.class) != null)
                    .map(f -> String.format("%s__$.%s(%s__$);", objectInfo.getObjectName(), f.getSetter().getSimpleName(), f.getName()))
                    .toList());
            return outputStatements;
        }
    }

    private List<String> handleMultipleOutputs(ObjectInfo objectInfo, List<OutputElement> outputElements) {
        List<String> resultSetStmt = new ArrayList<>();
        int i = 1;
        for (var output : outputElements) {
            var type = JdbcHelper.fromSimpleName(output.element.asType().toString());
            output.pos = String.format("pos - %d + %d", outputElements.size(), i);
            if (output.wrappedType != null) {
                type = JdbcHelper.fromSimpleName(output.wrappedType.toString());
            }
            if (type == null) {
                var newObjectInfo = objectInfo
                        .getFieldInfoSet()
                        .stream()
                        .filter(f -> f.getField().equals(output.element))
                        .map(f -> {
                            if (!output.isWrapped()) {
                                return extractor.extractFields(f.getName(), getTypeElement(context, f.getField()));
                            } else {
                                var extractedInfo = extractor.extractFields(f.getName(), output.wrappedType);
                                extractedInfo.setWrapper(getTypeElement(context, output.element));
                                return extractedInfo;
                            }
                        })
                        .findFirst(); // TODO: handle wrapper  handle primitive types
                newObjectInfo.ifPresent(info -> resultSetStmt.add(handleOneObjectOutput(info, output)));
            } else {
                resultSetStmt.add(handleOneVariableOutput(new VariableInfo(output.element), output));
            }
            i++;
        }
        return resultSetStmt;
    }

    private String handleOneVariableOutput(VariableInfo variableInfo, OutputElement outputElement) {
        ST template = new ST(SIMPLE_OUTPUT_TEMPLATE);
        if (outputElement.isWrapped()) {
            template = new ST(OPTIONAL_SIMPLE_OUTPUT_TEMPLATE);
            template.add(POSITION.name(), outputElement.pos);
            var type = JdbcHelper.fromSimpleName(outputElement.wrappedType.toString());
            template.add(STMT_RESULT_TYPE.name(), String.format("java.util.Optional<%s>", outputElement.wrappedType));
            template.add(STMT_GETTER.name(), Objects.requireNonNull(type).getJdbcGetterMethod());
            template.add(OBJECT_INIT_STATEMENT.name(), String.format("%s__$", variableInfo.getName()));
        } else {
            template.add(POSITION.name(), outputElement.pos);
            template.add(STMT_RESULT_TYPE.name(), variableInfo.getTypeName());
            template.add(STMT_GETTER.name(), variableInfo.getJdbcMappedType().getJdbcGetterMethod());
            template.add(OBJECT_INIT_STATEMENT.name(), String.format("%s__$", variableInfo.getName()));
        }
        return template.render();
    }

    private String handleOneObjectOutput(ObjectInfo objectInfo, OutputElement outputElement) {
        ST template;
        boolean isWrappedInList = objectInfo.isWrapped() && elementTools.isCollection(objectInfo.getWrapper());
        boolean isWrappedInOptional = objectInfo.isWrapped() && elementTools.isOptional(objectInfo.getWrapper());
        boolean isWrapped = isWrappedInOptional || isWrappedInList;
        if (isWrappedInList) { // TODO : handle primitive types
            template = new ST(LIST_OUTPUT_TEMPLATE);
            template.add(WRAPPER_INIT_STATEMENT.name(), String.format("%s<%s> %s__$ = new %s<>();",
                    objectInfo.getWrapper().getQualifiedName(),
                    objectInfo.getField().asType(),
                    objectInfo.getObjectName(),
                    elementTools.mapToConcreteClass(objectInfo.getWrapper()).getQualifiedName()));
            template.add(ADD_OBJECT_STATEMENT.name(), String.format("%s__$.add(%s__$);",
                    objectInfo.getObjectName(),
                    objectInfo.getWrappedUpName()));
        } else if (isWrappedInOptional) { // TODO : handle primitive types
            template = new ST(OPTIONAL_OUTPUT_TEMPLATE);
            template.add(WRAPPER_INIT_STATEMENT.name(), String.format("java.util.Optional<%s> %s__$ = java.util.Optional.of(%s__$);",
                    outputElement.wrappedType.asType(),
                    objectInfo.getObjectName(),
                    objectInfo.getWrappedUpName()));
            template.add(HANDLE_EMPTY_STATEMENT.name(), String.format("%s__$ = java.util.Optional.empty();", objectInfo.getObjectName()));
        } else {
            template = new ST(ONE_OUTPUT_TEMPLATE);
        }
        template.add(POSITION.name(), outputElement.pos); // pass position in outputElement

        ExecutableElement constructor;
        if (isWrapped) {
            constructor = elementTools.extractConstructor(outputElement.wrappedType);
        } else {
            constructor = elementTools.extractConstructor(outputElement.element);
        }
        if (constructor != null) {
            template.add(OBJECT_INIT_STATEMENT.name(),
                    initObject(isWrapped ? outputElement.wrappedType : outputElement.element,
                            isWrapped ? objectInfo.getWrappedUpName() : objectInfo.getObjectName()));
        } else {
            throw new IllegalStateException("Default constructor is not present (accepts constructor without parameters only)");
        }
        template.add(HANDLE_EMPTY_STATEMENT.name(), "");
        template.add(STMT_RESULT_TYPE.name(), "java.sql.ResultSet");
        template.add(STMT_GETTER.name(), "getObject");

        List<String> statements = new ArrayList<>();
        for (var fieldInfo : objectInfo.getFieldInfoSet()) {
            var setter = fieldInfo.getSetter().getSimpleName().toString();
            if (fieldInfo.isWrapped()) {
                throw new IllegalStateException("Unsupported type");
            }
            if (fieldInfo.isSimple()) {
                var resultSet = resultSetSetter(fieldInfo);
                statements.add(String.format("%s__$.%s(%s);", isWrapped ? objectInfo.getWrappedUpName() : objectInfo.getObjectName(), setter, resultSet));
            } else {
                List<String> initSetStack = new ArrayList<>();
                flattenObjectInfo(objectInfo, fieldInfo, initSetStack, isWrapped ? objectInfo.getWrappedUpName() : objectInfo.getObjectName());
                statements.addAll(initSetStack);
            }
        }
        template.add(SETTER_STATEMENTS.name(), statements);
        return template.render();
    }

    private String initObject(Element type, String name) {
        return String.format("%s %s__$ = new %s();", type.asType(), name, type.asType());
    }

    private String resultSetSetter(VariableInfo fieldInfo) {
        String rs = String.format("rs.%s(\"%s\")",
                fieldInfo.getJdbcMappedType().getJdbcGetterMethod(),
                CaseConverter.toSnakeCase(fieldInfo.getName()));
        if (fieldInfo.getJdbcMappedType().isDateTime()) {
            return String.format("%s != null ? %s.%s : null",
                    rs, rs, toLocaleDate(fieldInfo.getJdbcMappedType()));
        } else if (fieldInfo.getJdbcMappedType() == CHARACTER_WRAPPER || fieldInfo.getJdbcMappedType() == CHARACTER){
            return String.format("%s != null ? %s.%s : null",
                    rs, rs, "charAt(0)");
        }
        return rs;
    }

    private String toLocaleDate(JdbcHelper jdbcType) {
        if (jdbcType == JdbcHelper.LOCAL_DATE) {
            return "toLocalDate()";
        } else if (jdbcType == JdbcHelper.LOCAL_DATE_TIME) {
            return "toLocalDateTime()";
        } else if (jdbcType == JdbcHelper.LOCAL_TIME) {
            return "toLocalTime()";
        }
        return null;
    }

    private void validateField(FieldInfo fieldInfo) {
        if (elementTools.isIterator(fieldInfo.getField())) {
            throw new IllegalStateException("Unsupported List field must be a simple object or primitive");
        }
    }

    private void flattenObjectInfo(ObjectInfo objectInfo, FieldInfo parentField, List<String> list, String currentObj) {
        TypeElement typeElement = getTypeElement(context, parentField.getField());
        var fieldInfoSet = objectInfo.getNestedObjects().get(typeElement);
        var initObject = initObject(typeElement, parentField.getName());
        list.add(initObject);
        list.add(String.format("%s__$.%s(%s__$);", currentObj, parentField.getSetter().getSimpleName(), parentField.getName()));
        if (fieldInfoSet != null) {
            for (var field : fieldInfoSet) {
                if (field.isSimple()) {
                    var resultSet = resultSetSetter(field);
                    list.add(String.format("%s__$.%s(%s);", parentField.getName(), field.getSetter().getSimpleName(), resultSet));
                } else {
                    flattenObjectInfo(objectInfo, field, list, parentField.getName());
                }
            }
        }
    }
}
