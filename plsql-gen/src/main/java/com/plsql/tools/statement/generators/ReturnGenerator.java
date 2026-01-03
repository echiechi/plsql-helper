package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.statement.Generator;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.plsql.tools.enums.TypeMapper.CHARACTER;
import static com.plsql.tools.enums.TypeMapper.CHARACTER_WRAPPER;
import static com.plsql.tools.tools.CodeGenConstants.*;
import static com.plsql.tools.tools.Tools.defaultInitTypeForList;

public class ReturnGenerator implements Generator {
    // TODO: less string literals and more in constants ? create a class for this purpose with methods extr ?
    public static final String GET_OBJECT = "getObject";

    enum TemplateParams {
        POSITION,
        OBJECT_INIT_STATEMENT,
        STMT_RESULT_VAR,
        STMT_RESULT_TYPE,
        STMT_VAR_NAME,
        STMT_GETTER,
        HANDLE_EMPTY_STATEMENT,
        OPTIONAL_TYPE,
        SETTER_STATEMENTS;
    }

    public static final String SIMPLE_RETURN_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = <STMT_VAR_NAME>.<STMT_GETTER>(<POSITION>);
             """;
    public static final String OPTIONAL_RETURN_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = <OPTIONAL_TYPE>.of(<STMT_VAR_NAME>.<STMT_GETTER>(<POSITION>));
             """;

    public static final String OBJECT_RETURN_TEMPLATE = """
            try(<STMT_RESULT_TYPE> <STMT_RESULT_VAR> = (<STMT_RESULT_TYPE>)<STMT_VAR_NAME>.<STMT_GETTER>(<POSITION>);){
                if (!<STMT_RESULT_VAR>.next()) {
                            <HANDLE_EMPTY_STATEMENT>
                    } else {
                        do {
                            <SETTER_STATEMENTS>
                        } while (<STMT_RESULT_VAR>.next());
                    }
            }""";
    private final List<ReturnElementInfo> returnElements;

    private final Extractor extractor;

    public ReturnGenerator(List<ReturnElementInfo> returnElements,
                           ProcessingContext context) {
        this.returnElements = returnElements;
        this.extractor = new Extractor(context);
    }

    @Override
    public String generate() {
        if (returnElements == null) {
            return "";
        }
        boolean isMultiOutput = returnElements.size() > 1;
        StringBuilder sb = new StringBuilder();
        for (int i = returnElements.size() - 1; i >= 0; i--) {
            var returnElement = returnElements.get(i);
            if (i != returnElements.size() - 1) {
                returnElement.setPos("--" + returnElement.getPos());
            }
            if (returnElement.hasRedundantType()) {
                throw new IllegalStateException("Redundant type is not yet supported (the usage of the same class type twice in a return type)");
            }
            var typeInfo = returnElement.getTypeInfo();
            if (typeInfo.isSimple()) {
                sb.append(handleSimpleReturn(returnElement))
                        .append("\n");
            } else if (!typeInfo.isWrapped()) {
                sb.append(handleComposedElement(returnElement))
                        .append("\n");
            } else {
                var isSimpleWrappedType = TypeMapper.isSimple(returnElement.getTypeInfo().wrappedTypeAsString());
                var isOptional = extractor.isOptional(typeInfo.getMirror());
                if (isOptional && isSimpleWrappedType) {
                    sb.append(handleSimpleOptionalReturn(returnElement))
                            .append("\n");
                } else if (isOptional) {
                    sb.append(handleOptionalComposedElement(returnElement)).append("\n");
                } else if (extractor.isCollection(typeInfo.getMirror())) {
                    sb.append(handleCollectionReturn(returnElement))
                            .append("\n");
                }
            }
        }
        // multi output
        if (isMultiOutput) {
            var parent = returnElements.get(0).getParent();
            sb.append(initObject(parent))
                    .append("\n");
            extractor
                    .getAttachedElements(parent.getTypeInfo().getMirror())
                    .forEach(a -> {
                        var setter = String.format("%s.%s(%s);",
                                variableName(parent.getName()),
                                a.getSetter().getSimpleName(),
                                variableName(a.getName()));
                        sb.append(setter).append("\n");
                    });

        }
        return sb.toString();
    }

    private String handleOptionalComposedElement(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), java.sql.ResultSet.class.getCanonicalName());
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "return %s.empty();"
                .formatted(Optional.class.getCanonicalName()));
        template.add(TemplateParams.STMT_VAR_NAME.name(), CodeGenConstants.STATEMENT_VAR);
        template.add(TemplateParams.STMT_RESULT_VAR.name(), RESULT_SET_VAR);

        var defaultReturnName = returnElementInfo.getName();
        returnElementInfo.setName(wrappedVariableName(defaultReturnName)); // setting new objectsName;
        var optionalAssign = String.format("%s = %s.of(%s);",
                variableName(defaultReturnName),
                Optional.class.getCanonicalName(),
                variableName(returnElementInfo.getName()));
        template.add(TemplateParams.SETTER_STATEMENTS.name(),
                String.join("\n", flattenToStatements(returnElementInfo, false)) + "\n" + optionalAssign
        );
        String renderedTemplate = template.render();
        var optionalInit = String.format("%s<%s> %s = %s.empty();",
                Optional.class.getCanonicalName(),
                returnElementInfo.getTypeInfo().wrappedTypeAsString(),
                variableName(defaultReturnName),
                Optional.class.getCanonicalName());
        return optionalInit + "\n" + renderedTemplate;
    }

    private String handleComposedElement(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), java.sql.ResultSet.class.getCanonicalName());
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "return %s;".formatted(variableName(returnElementInfo.getName())));
        template.add(TemplateParams.SETTER_STATEMENTS.name(), String.join("\n", flattenToStatements(returnElementInfo, true)));
        template.add(TemplateParams.STMT_VAR_NAME.name(), CodeGenConstants.STATEMENT_VAR);
        template.add(TemplateParams.STMT_RESULT_VAR.name(), RESULT_SET_VAR);

        return initObjectToNull(returnElementInfo) + "\n" +
                template.render();
    }

    private String handleSimpleReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(SIMPLE_RETURN_TEMPLATE);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), returnElementInfo.getTypeInfo().typeAsString());
        template.add(TemplateParams.STMT_GETTER.name(), returnElementInfo.getTypeInfo().asTypeMapper().getJdbcGetterMethod());
        template.add(TemplateParams.OBJECT_INIT_STATEMENT.name(), String.format("%s", variableName(returnElementInfo.getName())));
        template.add(TemplateParams.STMT_VAR_NAME.name(), CodeGenConstants.STATEMENT_VAR);
        template.add(TemplateParams.STMT_RESULT_VAR.name(), RESULT_SET_VAR);

        return template.render();
    }

    private String handleSimpleOptionalReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OPTIONAL_RETURN_TEMPLATE);
        var typeInfo = returnElementInfo.getTypeInfo();
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), String.format("%s<%s>",
                Optional.class.getCanonicalName(),
                typeInfo.wrappedTypeAsString()));
        template.add(TemplateParams.STMT_GETTER.name(), Objects.requireNonNull(typeInfo.wrappedTypeAsTypeMapper()).getJdbcGetterMethod());
        template.add(TemplateParams.OBJECT_INIT_STATEMENT.name(), String.format("%s", variableName(returnElementInfo.getName())));
        template.add(TemplateParams.OPTIONAL_TYPE.name(), Optional.class.getCanonicalName());
        template.add(TemplateParams.STMT_VAR_NAME.name(), CodeGenConstants.STATEMENT_VAR);
        return template.render();
    }

    private String handleCollectionReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), java.sql.ResultSet.class.getCanonicalName());
        template.add(TemplateParams.STMT_RESULT_VAR.name(), RESULT_SET_VAR);
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "");
        template.add(TemplateParams.STMT_VAR_NAME.name(), CodeGenConstants.STATEMENT_VAR);
        var defaultReturnName = returnElementInfo.getName();
        returnElementInfo.setName(wrappedVariableName(defaultReturnName)); // setting new objectsName;
        var addObjectToList = String.format("%s.add(%s);",
                variableName(defaultReturnName),
                variableName(returnElementInfo.getName()));
        template.add(TemplateParams.SETTER_STATEMENTS.name(),
                String.join("\n", flattenToStatements(returnElementInfo, false)) +
                        "\n" + addObjectToList
        );
        String renderedTemplate = template.render();
        TypeMirror listType = extractor.eraseType(returnElementInfo.getTypeInfo().getMirror());
        // init list/set
        var listInit = String.format("%s<%s> %s = new %s<>();",
                listType.toString(),
                returnElementInfo.getTypeInfo().wrappedTypeAsString(),
                variableName(defaultReturnName),
                defaultInitTypeForList(listType));
        return listInit + "\n" + renderedTemplate;
    }

    private List<String> flattenToStatements(ComposedElementInfo composedElementInfo, boolean isAssign) {
        var statements = flattenComposedElementInfo(composedElementInfo);
        if (composedElementInfo.getTypeInfo().isRecord()) {
            statements.addAll(initRecordStatements(composedElementInfo, isAssign));
        } else {
            statements.addAll(initObjectStatements(composedElementInfo, isAssign));
        }
        return statements;
    }

    private List<String> initRecordStatements(ComposedElementInfo composedElementInfo, boolean isAssign) {
        var statements = new ArrayList<String>();
        var typeInfo = composedElementInfo.getTypeInfo();
        String type = typeInfo.isWrapped() ? typeInfo.wrappedTypeAsString() : typeInfo.typeAsString();
        String parameters = composedElementInfo.getElementInfoList()
                .stream()
                .map(e -> String.format("%s", variableName(e.getName())))
                .collect(Collectors.joining(", "));
        if (isAssign) {
            statements.add(assignToObject(composedElementInfo, parameters));
        } else {
            statements.add(String.format("%s %s = new %s(%s);", type,
                    variableName(composedElementInfo.getName()),
                    type, parameters)
            );
        }
        return statements;
    }

    private List<String> initObjectStatements(ComposedElementInfo composedElementInfo, boolean isAssign) {
        var statements = new ArrayList<String>();
        if (isAssign) {
            statements.add(assignToObject(composedElementInfo, ""));
        } else {
            statements.add(initObject(composedElementInfo));
        }
        for (var e : composedElementInfo.getElementInfoList()) {
            statements.add(String.format("%s.%s(%s);",
                    variableName(composedElementInfo.getName()),
                    e.getSetter().getSimpleName(),
                    variableName(e.getName())));
        }
        return statements;
    }

    private List<String> flattenComposedElementInfo(ComposedElementInfo composedElementInfo) {
        List<String> statements = new ArrayList<>();
        for (var attachedElementInfo : composedElementInfo.getElementInfoList()) {
            if (attachedElementInfo.getTypeInfo().isSimple()) {
                String resultSet = resultSetGetter(attachedElementInfo);
                statements.add(String.format("%s %s = %s;",
                        attachedElementInfo.getTypeInfo().typeAsString(),
                        variableName(attachedElementInfo.getName()),
                        resultSet
                ));
            } else {
                var typeInfo = attachedElementInfo.getTypeInfo();
                var extractedElement = extractor
                        .convertInto(typeInfo.getRawType());
                statements.addAll(flattenComposedElementInfo(extractedElement));
                if (extractedElement.getTypeInfo().isRecord()) {
                    statements.addAll(initRecordStatements(extractedElement, false));
                } else {
                    statements.addAll(initObjectStatements(extractedElement, false));
                }
            }
        }
        return statements;
    }

    private String assignToObject(ElementInfo elementInfo, String parameters) {
        var typeInfo = elementInfo.getTypeInfo();
        return String.format("%s = new %s(%s);",
                variableName(elementInfo.getName()),
                typeInfo.typeAsString(),
                parameters);
    }

    private String initObjectToNull(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        return String.format("%s %s = null;", typeInfo.typeAsString(), variableName(elementInfo.getName()));

    }

    private String initObject(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        if (typeInfo.isWrapped()) {
            return initObject(typeInfo.wrappedTypeAsString(), elementInfo.getName());
        }
        return initObject(typeInfo.typeAsString(), elementInfo.getName());
    }

    private String initObject(String typeAsString, String name) {
        return String.format("%s %s = new %s();", typeAsString, variableName(name), typeAsString);
    }

    private String resultSetGetter(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        String rs = String.format("%s.%s(\"%s\")",
                RESULT_SET_VAR,
                typeInfo.asTypeMapper().getJdbcGetterMethod(),
                CaseConverter.toSnakeCase(elementInfo.getName()));
        if (typeInfo.asTypeMapper().isDateTime()) {
            return toLocaleDate(typeInfo.asTypeMapper(), rs);
        } else if (typeInfo.asTypeMapper() == CHARACTER_WRAPPER || typeInfo.asTypeMapper() == CHARACTER) {
            return String.format("StringTools.toChar(%s)", rs);
        }
        return rs;
    }

    private String toLocaleDate(TypeMapper jdbcType, String resultSetStatement) {
        if (jdbcType == TypeMapper.LOCAL_DATE) {
            return "DateTools.toLocalDate(%s)".formatted(resultSetStatement);
        } else if (jdbcType == TypeMapper.LOCAL_DATE_TIME) {
            return "DateTools.toLocalDateTime(%s)".formatted(resultSetStatement);
        } else if (jdbcType == TypeMapper.LOCAL_TIME) {
            return "DateTools.toLocalTime(%s)".formatted(resultSetStatement);
        }
        throw new IllegalArgumentException("No mapping provided for the specified type " + jdbcType.getDisplayName());
    }
}
