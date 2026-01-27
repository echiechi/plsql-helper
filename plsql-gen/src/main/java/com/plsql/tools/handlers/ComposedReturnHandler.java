package com.plsql.tools.handlers;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.templates.CodeSnippets;
import com.plsql.tools.templates.ReturnCodeTemplateManager;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.plsql.tools.enums.TypeMapper.CHARACTER;
import static com.plsql.tools.enums.TypeMapper.CHARACTER_WRAPPER;
import static com.plsql.tools.tools.CodeGenConstants.*;

public class ComposedReturnHandler implements ReturnTypeHandler {
    private static final Map<TypeMapper, Function<String, String>> TRANSFORMERS = Map.of(
            TypeMapper.DATE, GenTools::toDate,
            TypeMapper.LOCAL_DATE, GenTools::toLocalDate,
            TypeMapper.LOCAL_DATE_TIME, GenTools::toLocalDateTime,
            TypeMapper.LOCAL_TIME, GenTools::toLocalTime
    );
    private final Extractor extractor;
    private final ReturnCodeTemplateManager returnCodeTemplateManager = new ReturnCodeTemplateManager();
    private boolean isInitObject = true;
    private boolean isToAssign = true;
    private boolean isWrapped = false;
    private boolean isReturnSomething = true;
    private String toAppendToStatements = "";

    public ComposedReturnHandler(Extractor extractor,
                                 boolean isToAssign,
                                 boolean isWrapped,
                                 boolean isInitObject,
                                 boolean isReturnSomething,
                                 String toAppendToStatements) {
        this.extractor = extractor;
        this.isToAssign = isToAssign;
        this.isWrapped = isWrapped;
        this.isInitObject = isInitObject;
        this.isReturnSomething = isReturnSomething;
        this.toAppendToStatements = toAppendToStatements;
    }

    public ComposedReturnHandler(Extractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public boolean canHandle(ReturnElementInfo returnElement) {
        return !returnElement.getTypeInfo().isWrapped() && !returnElement.getTypeInfo().isSimple();
    }

    @Override
    public String generateCode(ReturnElementInfo returnElement) {
        Map<CodeSnippets.ResultSetParams, String> context = createContextForProcessingResultSet(
                returnElement.getPos(),
                isReturnSomething ? GenTools.returnObject(variableName(returnElement.getName())) : "",
                String.join("\n", flattenToStatements(returnElement, isToAssign, isWrapped))
                        + "\n" + toAppendToStatements
        );
        return (isInitObject ? initObjectToNull(returnElement) + "\n" : "") +
                returnCodeTemplateManager.renderProcessResultSet(context);
    }

    private List<String> flattenToStatements(ComposedElementInfo composedElementInfo,
                                             boolean isAssign,
                                             boolean isWrapped
    ) {
        if (isWrapped) {
            composedElementInfo.setName(wrappedVariableName(composedElementInfo.getName()));
        }
        var statements = flattenComposedElementInfo(composedElementInfo);
        if (composedElementInfo.getTypeInfo().isRecord()) {
            statements.addAll(initRecordStatements(composedElementInfo, isAssign));
        } else {
            statements.addAll(initObjectStatements(composedElementInfo, isAssign));
        }
        return statements;
    }

    private List<String> flattenComposedElementInfo(ComposedElementInfo composedElementInfo) {
        List<String> statements = new ArrayList<>();
        for (var attachedElementInfo : composedElementInfo.getElementInfoList()) {
            if (attachedElementInfo.getTypeInfo().isSimple()) {
                String resultSet = resultSetGetter(attachedElementInfo);
                statements.add(
                        GenTools.assignAndInit(
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

    private List<String> initRecordStatements(ComposedElementInfo composedElementInfo, boolean isAssign) {
        var statements = new ArrayList<String>();
        var typeInfo = composedElementInfo.getTypeInfo();
        String type = typeInfo.isWrapped() ? typeInfo.wrappedTypeAsString() : typeInfo.typeAsString();
        String parameters = composedElementInfo.getElementInfoList()
                .stream()
                .map(e -> variableName(e.getName()))
                .collect(Collectors.joining(", "));
        if (isAssign) {
            // example: name = new Type(params..);
            statements.add(assignNewObject(composedElementInfo, parameters));
        } else {
            // example: Type name = new Type(params..);
            statements.add(GenTools.assignAndInitObject(type, variableName(composedElementInfo.getName()), parameters));
        }
        return statements;
    }

    private List<String> initObjectStatements(ComposedElementInfo composedElementInfo, boolean isAssign) {
        var statements = new ArrayList<String>();
        if (isAssign) {
            statements.add(assignNewObject(composedElementInfo, ""));
        } else {
            statements.add(GenTools.initObject(composedElementInfo));
        }
        for (var e : composedElementInfo.getElementInfoList()) {
            statements.add(
                    GenTools.constructMethod(
                            variableName(composedElementInfo.getName()),
                            e.getSetter().getSimpleName().toString(),
                            variableName(e.getName())
                    ).concat(";")
            );
        }
        return statements;
    }

    private String initObjectToNull(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        return GenTools.assignNullAndInit(typeInfo.typeAsString(), variableName(elementInfo.getName()));
    }

    //example : name = new Type(param);
    private String assignNewObject(ElementInfo elementInfo, String parameters) {
        var typeInfo = elementInfo.getTypeInfo();
        return GenTools.assignNewObject(
                typeInfo.typeAsString(),
                variableName(elementInfo.getName()),
                parameters
        );
    }

    private String resultSetGetter(ElementInfo elementInfo) {
        var typeInfo = elementInfo.getTypeInfo();
        String resultSet = GenTools.constructMethod(RESULT_SET_VAR,
                typeInfo.asTypeMapper().getJdbcGetterMethod(),
                GenTools.literalString(elementInfo.getAlias())
        );
        if (typeInfo.asTypeMapper().isDateTime()) {
            return TRANSFORMERS.get(typeInfo.asTypeMapper()).apply(resultSet);
        } else if (typeInfo.asTypeMapper() == CHARACTER_WRAPPER || typeInfo.asTypeMapper() == CHARACTER) {
            return GenTools.toChar(resultSet);
        }
        return resultSet;
    }

    private Map<CodeSnippets.ResultSetParams, String> createContextForProcessingResultSet(
            String position,
            String emptyStatement,
            String setterStatements
    ) {
        return Map.of(
                CodeSnippets.ResultSetParams.STMT_RESULT_TYPE, java.sql.ResultSet.class.getCanonicalName(),
                CodeSnippets.ResultSetParams.STMT_GETTER, TypeMapper.OBJECT.getJdbcGetterMethod(),
                CodeSnippets.ResultSetParams.POSITION, position,
                CodeSnippets.ResultSetParams.HANDLE_EMPTY_STATEMENT, emptyStatement,
                CodeSnippets.ResultSetParams.STMT_VAR_NAME, CodeGenConstants.STATEMENT_VAR,
                CodeSnippets.ResultSetParams.STMT_RESULT_VAR, RESULT_SET_VAR,
                CodeSnippets.ResultSetParams.SETTER_STATEMENTS, setterStatements
        );
    }
}
