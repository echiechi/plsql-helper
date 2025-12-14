package com.plsql.tools.statement.generators;

import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.statement.Generator;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.plsql.tools.enums.JdbcHelper.CHARACTER;
import static com.plsql.tools.enums.JdbcHelper.CHARACTER_WRAPPER;
import static com.plsql.tools.utils.CaseConverter.upperCaseFirstLetter;

public class ReturnGenerator implements Generator {

    public static final String SQL_RESULT_SET = "java.sql.ResultSet";
    public static final String GET_OBJECT = "getObject";

    enum TemplateParams {
        POSITION,
        OBJECT_INIT_STATEMENT,
        STMT_RESULT_TYPE,
        STMT_GETTER,
        HANDLE_EMPTY_STATEMENT,
        SETTER_STATEMENTS;
    }

    public static final String SIMPLE_RETURN_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = stmt.<STMT_GETTER>(<POSITION>);
             """;
    public static final String OPTIONAL_RETURN_TEMPLATE = """
            <STMT_RESULT_TYPE> <OBJECT_INIT_STATEMENT> = java.util.Optional.of(stmt.<STMT_GETTER>(<POSITION>));
             """;

    public static final String OBJECT_RETURN_TEMPLATE = """
            try(<STMT_RESULT_TYPE> rs = (<STMT_RESULT_TYPE>)stmt.<STMT_GETTER>(<POSITION>);){
                if (!rs.next()) {
                            <HANDLE_EMPTY_STATEMENT>
                    } else {
                        do {
                            <SETTER_STATEMENTS>
                        } while (rs.next());
                    }
            }""";
    private final List<ReturnElementInfo> returnElements;

    public ReturnGenerator(List<ReturnElementInfo> returnElements) {
        this.returnElements = returnElements;
    }

    @Override
    public String generate() {
        if (returnElements == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = returnElements.size() - 1; i >= 0; i--) {
            var returnElement = returnElements.get(i);
            if (i != returnElements.size() - 1) {
                returnElement.setPos("--" + returnElement.getPos());
            }
            var typeInfo = returnElement.getTypeInfo();
            if (typeInfo.isSimple()) {
                sb.append(handleSimpleReturn(returnElement))
                        .append("\n");
            } else if (!typeInfo.isWrapped()) {
                sb.append(handleComposedElement(returnElement))
                        .append("\n");
            } else {
                var isSimpleWrappedType = TypeInfo.isSimple(returnElement.getTypeInfo().getWrappedType());
                var isOptional = Extractor.getInstance().isOptional(typeInfo.getMirror());
                if (isOptional && isSimpleWrappedType) {
                    sb.append(handleSimpleOptionalReturn(returnElement))
                            .append("\n");
                } else if (isOptional) {
                    sb.append(handleOptionalComposedElement(returnElement)).append("\n");
                } else if (Extractor.getInstance().isCollection(typeInfo.getMirror())) {
                    sb.append(handleCollectionReturn(returnElement))
                            .append("\n");
                }
            }
        }
        // multi output
        if (returnElements.size() > 1) {
            var parent = returnElements.get(0).getParent();
            sb.append(initObject(parent))
                    .append("\n");
            Extractor.getInstance()
                    .getAttachedElements(parent.getTypeInfo().getMirror())
                    .forEach(a -> {
                        var setter = String.format("%s__$.%s(%s__$);", parent.getName(), a.getSetter().getSimpleName(), a.getName());
                        sb.append(setter).append("\n");
                    });

        }
        return sb.toString();
    }

    private String handleOptionalComposedElement(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), SQL_RESULT_SET);
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "return java.util.Optional.empty();");
        var defaultReturnName = returnElementInfo.getName();
        returnElementInfo.setName("wrapped%s".formatted(upperCaseFirstLetter(defaultReturnName))); // setting new objectsName;
        var optionalAssign = String.format("%s__$ = java.util.Optional.of(%s__$);", defaultReturnName, returnElementInfo.getName());
        template.add(TemplateParams.SETTER_STATEMENTS.name(),
                String.join("\n", flattenToStatements(returnElementInfo, false)) + "\n" + optionalAssign
        );
        String renderedTemplate = template.render();
        var optionalInit = String.format("java.util.Optional<%s> %s__$ = java.util.Optional.empty();",
                returnElementInfo.getTypeInfo().wrappedTypeAsString(),
                defaultReturnName);
        return optionalInit + "\n" + renderedTemplate;
    }

    private String handleComposedElement(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), SQL_RESULT_SET);
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "return %s__$;".formatted(returnElementInfo.getName()));
        template.add(TemplateParams.SETTER_STATEMENTS.name(), String.join("\n", flattenToStatements(returnElementInfo, true)));
        return initObjectToNull(returnElementInfo) + "\n" +
                template.render();
    }

    private String handleSimpleReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(SIMPLE_RETURN_TEMPLATE);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), returnElementInfo.getTypeInfo().typeAsString());
        template.add(TemplateParams.STMT_GETTER.name(), returnElementInfo.getTypeInfo().asJdbcHelper().getJdbcGetterMethod());
        template.add(TemplateParams.OBJECT_INIT_STATEMENT.name(), String.format("%s__$", returnElementInfo.getName()));
        return template.render();
    }

    private String handleSimpleOptionalReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OPTIONAL_RETURN_TEMPLATE);
        var typeInfo = returnElementInfo.getTypeInfo();
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), String.format("java.util.Optional<%s>", typeInfo.wrappedTypeAsString()));
        template.add(TemplateParams.STMT_GETTER.name(), Objects.requireNonNull(typeInfo.wrappedTypeAsJdbcHelper()).getJdbcGetterMethod());
        template.add(TemplateParams.OBJECT_INIT_STATEMENT.name(), String.format("%s__$", returnElementInfo.getName()));
        return template.render();
    }

    private String handleCollectionReturn(ReturnElementInfo returnElementInfo) {
        ST template = new ST(OBJECT_RETURN_TEMPLATE);
        template.add(TemplateParams.STMT_RESULT_TYPE.name(), SQL_RESULT_SET);
        template.add(TemplateParams.STMT_GETTER.name(), GET_OBJECT);
        template.add(TemplateParams.POSITION.name(), returnElementInfo.getPos());
        template.add(TemplateParams.HANDLE_EMPTY_STATEMENT.name(), "");
        var defaultReturnName = returnElementInfo.getName();
        returnElementInfo.setName("wrapped%s".formatted(upperCaseFirstLetter(defaultReturnName))); // setting new objectsName;
        var addObjectToList = String.format("%s__$.add(%s__$);", defaultReturnName, returnElementInfo.getName());
        template.add(TemplateParams.SETTER_STATEMENTS.name(),
                String.join("\n", flattenToStatements(returnElementInfo, false)) +
                        "\n" + addObjectToList
        );
        String renderedTemplate = template.render();
        var listInit = String.format("java.util.List<%s> %s__$ = new java.util.ArrayList<>();",
                returnElementInfo.getTypeInfo().wrappedTypeAsString(),
                defaultReturnName);
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
                .map(e -> String.format("%s__$", e.getName()))
                .collect(Collectors.joining(", "));
        if (isAssign) {
            statements.add(assignToObject(composedElementInfo, parameters));
        } else {
            statements.add(String.format("%s %s__$ = new %s(%s);", type,
                    composedElementInfo.getName(),
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
            statements.add(String.format("%s__$.%s(%s__$);",
                    composedElementInfo.getName(),
                    e.getSetter().getSimpleName(),
                    e.getName()));
        }
        return statements;
    }

    private List<String> flattenComposedElementInfo(ComposedElementInfo composedElementInfo) {
        List<String> statements = new ArrayList<>();
        for (var attachedElementInfo : composedElementInfo.getElementInfoList()) {
            if (attachedElementInfo.getTypeInfo().isSimple()) {
                String resultSet = resultSetGetter(attachedElementInfo);
                statements.add(String.format("%s %s__$ = %s;",
                        attachedElementInfo.getTypeInfo().typeAsString(),
                        attachedElementInfo.getName(),
                        resultSet
                ));
            } else {
                var typeInfo = attachedElementInfo.getTypeInfo();
                var extractedElement = Extractor.getInstance()
                        .convertInto(typeInfo.getRawType());
                statements.addAll(flattenComposedElementInfo(extractedElement));
                if (extractedElement.getTypeInfo().isRecord()) {
                    statements.addAll(initRecordStatements(extractedElement, false));
                } else {
                    statements.addAll(initObjectStatements(extractedElement, false));
                }
                /*statements.add(initObject(attachedElementInfo));
                for (var e : extractedElement.getElementInfoList()) {
                    statements.add(String.format("%s__$.%s(%s__$);",
                            attachedElementInfo.getName(),
                            e.getSetter().getSimpleName(),
                            e.getName()));
                }*/
            }
        }
        return statements;
    }

    private String assignToObject(ElementInfo elementInfo, String parameters) {
        var typeInfo = elementInfo.getTypeInfo();
        return String.format("%s__$ = new %s(%s);", elementInfo.getName(), typeInfo.typeAsString(), parameters);
    }

    private String initObjectToNull(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        return String.format("%s %s__$ = null;", typeInfo.typeAsString(), elementInfo.getName());

    }

    private String initObject(ElementInfo elementInfo) { // TODO: default constructor ?
        var typeInfo = elementInfo.getTypeInfo();
        if (typeInfo.isWrapped()) {
            return initObject(typeInfo.wrappedTypeAsString(), elementInfo.getName());
        }
        return initObject(typeInfo.typeAsString(), elementInfo.getName());
    }

    private String initObject(String typeAsString, String name) {
        return String.format("%s %s__$ = new %s();", typeAsString, name, typeAsString);
    }

    private String resultSetGetter(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        String rs = String.format("rs.%s(\"%s\")",
                typeInfo.asJdbcHelper().getJdbcGetterMethod(),
                CaseConverter.toSnakeCase(elementInfo.getName()));
        if (typeInfo.asJdbcHelper().isDateTime()) {
            return String.format("%s != null ? %s%s : null",
                    rs, rs, toLocaleDate(typeInfo.asJdbcHelper()));
        } else if (typeInfo.asJdbcHelper() == CHARACTER_WRAPPER || typeInfo.asJdbcHelper() == CHARACTER) {
            return String.format("%s != null ? %s.%s : null", // TODO : helper method for more clarity ?
                    rs, rs, "charAt(0)");
        }
        return rs;
    }

    private String toLocaleDate(JdbcHelper jdbcType) {
        if (jdbcType == JdbcHelper.LOCAL_DATE) {
            return ".toLocalDate()";
        } else if (jdbcType == JdbcHelper.LOCAL_DATE_TIME) {
            return ".toLocalDateTime()";
        } else if (jdbcType == JdbcHelper.LOCAL_TIME) {
            return ".toLocalTime()";
        }
        return "";
    }
}
