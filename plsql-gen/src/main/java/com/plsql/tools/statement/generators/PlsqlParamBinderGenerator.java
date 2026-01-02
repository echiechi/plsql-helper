package com.plsql.tools.statement.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.statement.Generator;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.plsql.tools.tools.CodeGenConstants.POSITION_VAR;
import static com.plsql.tools.tools.CodeGenConstants.STATEMENT_VAR;

public class PlsqlParamBinderGenerator implements Generator {
    private final List<ElementInfo> methodParameters;
    private boolean isPreIncrement;
    private String statementName;

    private record ElementGetter(ElementInfo elementInfo, String getter) {
    }

    public PlsqlParamBinderGenerator(List<ElementInfo> methodParameters) {
        this.methodParameters = methodParameters;
    }

    @Override
    public String generate() {
        StringBuilder stringBuilder = new StringBuilder();
        for (var parameter : methodParameters) {
            if (parameter.getTypeInfo().isSimple()) {
                stringBuilder.append(handleSimpleParameter(parameter, parameter.getName())).append("\n");
            } else {
                stringBuilder
                        .append(handleComposedParameter((ComposedElementInfo) parameter))
                        .append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private String handleComposedParameter(ComposedElementInfo parameter) {
        StringBuilder stringBuilder = new StringBuilder();
        for (var attachedElementInfo : parameter.getElementInfoList()) {
            var type = attachedElementInfo.getTypeInfo();
            if (type.isSimple()) {
                String parameterGetter = selectGetter(parameter.getName(), attachedElementInfo);
                stringBuilder.append(handleSimpleParameter(attachedElementInfo, parameterGetter)).append("\n");
            } else {
                var flatElementInfoList = flattenComposedElementInfo(attachedElementInfo.getTypeInfo().getMirror(), parameter.getNestedElementInfo());
                for (var flatElement : flatElementInfoList) {
                    String fullGetter = appendToParent(parameter.getName() + "." + attachedElementInfo.getGetter(), flatElement.getter());
                    stringBuilder
                            .append(handleSimpleParameter(flatElement.elementInfo(), fullGetter))
                            .append("\n");
                }
            }
        }
        return stringBuilder.toString();
    }

    private String selectGetter(String parentName, AttachedElementInfo attachedElementInfo) {
        return attachedElementInfo.getGetter() != null ?
                appendToParent(parentName, attachedElementInfo.getGetter())
                : appendToParent(parentName, attachedElementInfo.getName());
    }

    private String appendToParent(String parentName, ExecutableElement getter) {
        return String.format("%s.%s()", parentName, getter.getSimpleName());
    }

    private String appendToParent(String parentName, String getter) {
        return String.format("%s.%s", parentName, getter);
    }

    private String handleSimpleParameter(ElementInfo parameter, String paramGetter) {
        if (paramGetter == null) {
            throw new IllegalArgumentException("param getter can't be null");
        }
        String finalParamGetter = paramGetter;
        var typeInfo = parameter.getTypeInfo();

        if (typeInfo.asTypeMapper().isDateTime()) {
            finalParamGetter = handleDateWithNullCheck(paramGetter, typeInfo.asTypeMapper());
        } else if (typeInfo.asTypeMapper() == TypeMapper.CHARACTER) {
            finalParamGetter = String.format("String.valueOf(%s)", paramGetter);
        }
        return bindParameter(statementName != null ? statementName : STATEMENT_VAR,
                typeInfo.asTypeMapper().getJdbcSetterMethod(),
                isPreIncrement ? "++%s".formatted(POSITION_VAR) : "%s++".formatted(POSITION_VAR),
                finalParamGetter);
    }

    private String bindParameter(String statement, String setter, String position, String parameter) {
        // example: stmt.setString(pos++, parameter1);
        return String.format("%s.%s(%s, %s);", statement, setter, position, parameter);
    }

    private String handleDateWithNullCheck(String dateObjGetter, TypeMapper type) {
        return switch (type) {
            case DATE, LOCAL_DATE -> String.format("DateTools.toSqlDate(%s)", dateObjGetter);
            case LOCAL_DATE_TIME -> String.format("DateTools.toTimestamp(%s)", dateObjGetter);
            case LOCAL_TIME -> String.format("DateTools.toTime(%s)", dateObjGetter);
            default -> dateObjGetter;
        };
    }

    private List<ElementGetter> flattenComposedElementInfo(TypeMirror typeMirror, Map<TypeMirror, List<AttachedElementInfo>> nestedElementInfo) {
        List<ElementGetter> flatGetters = new ArrayList<>();
        if (nestedElementInfo != null && nestedElementInfo.containsKey(typeMirror)) {
            for (var elementInfo : nestedElementInfo.get(typeMirror)) {
                if (elementInfo.getTypeInfo().isSimple()) {
                    flatGetters.add(new ElementGetter(elementInfo, elementInfo.getGetter().toString()));
                } else {
                    var extractions = flattenComposedElementInfo(elementInfo.getTypeInfo().getMirror(), nestedElementInfo);
                    var parentGetter = elementInfo.getGetter().toString();
                    flatGetters.addAll(extractions.stream()
                            .map(e -> new ElementGetter(e.elementInfo(), parentGetter + "." + e.getter()))
                            .toList());
                }
            }
        }
        return flatGetters;
    }

    public void setPreIncrement(boolean preIncrement) {
        isPreIncrement = preIncrement;
    }

    public void setStatementName(String statementName) {
        this.statementName = statementName;
    }
}
