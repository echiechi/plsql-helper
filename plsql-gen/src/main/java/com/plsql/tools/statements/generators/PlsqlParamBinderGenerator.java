package com.plsql.tools.statements.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.statements.Generator;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;

import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.CodeGenConstants.POSITION_VAR;
import static com.plsql.tools.tools.CodeGenConstants.STATEMENT_VAR;
import static com.plsql.tools.tools.GenTools.*;

public class PlsqlParamBinderGenerator implements Generator {

    private static final Map<TypeMapper, Function<String, String>> TRANSFORMERS = Map.of(
            TypeMapper.DATE, GenTools::toSqlDate,
            TypeMapper.LOCAL_DATE, GenTools::toSqlDate,
            TypeMapper.LOCAL_DATE_TIME, GenTools::toTimestamp,
            TypeMapper.LOCAL_TIME, GenTools::toTime
    );
    private final List<ElementInfo> methodParameters;
    private final boolean isPreIncrement;

    /**
     * Represents a flattened element with its complete getter path.
     * Used when recursively flattening nested composed types to track both
     * the element metadata and the full accessor chain.
     *
     * @param elementInfo The element metadata (type, name, etc.)
     * @param getter      The complete dot-separated path to access this element
     *                    Example: "parent.getChild().getValue"
     */
    private record ElementGetter(ElementInfo elementInfo, String getter) {
    }

    private record PendingElement(TypeMirror typeMirror, String parentPath) {
    }

    public PlsqlParamBinderGenerator(List<ElementInfo> methodParameters, boolean isPreIncrement) {
        this.methodParameters = methodParameters;
        this.isPreIncrement = isPreIncrement;
    }

    @Override
    public String generate() {
        if (methodParameters == null || methodParameters.isEmpty()) {
            return "";
        }
        List<String> generatedBindings = new ArrayList<>();
        for (var parameter : methodParameters) {
            if (parameter.getTypeInfo().isSimple()) {
                generatedBindings.add(simpleBinding(parameter, parameter.getName()));
            } else {
                generatedBindings.addAll(composedBinding((ComposedElementInfo) parameter));
            }
        }
        return String.join("\n", generatedBindings);
    }

    private List<String> composedBinding(ComposedElementInfo parameter) {
        return parameter.getElementInfoList().stream()
                .map(element -> generateBindingForElement(parameter, element))
                .toList();
    }

    private String generateBindingForElement(ComposedElementInfo parent, AttachedElementInfo element) {
        if (element.getTypeInfo().isSimple()) {
            return generateSimpleBinding(parent, element);
        }
        return generateNestedBinding(parent, element);
    }

    private String generateSimpleBinding(ComposedElementInfo parent, AttachedElementInfo element) {
        String parameterGetter = parameterPath(parent.getName(), element);
        return simpleBinding(element, parameterGetter);
    }

    private String generateNestedBinding(ComposedElementInfo parameter, AttachedElementInfo attachedElementInfo) {
        var flatElementInfoList = flattenComposedElementInfo(
                attachedElementInfo.getTypeInfo().getMirror(),
                parameter.getNestedElementInfo());
        return flatElementInfoList
                .stream()
                .map(flatElement -> {
                    var getter = attachedElementInfo.getGetter();
                    String fullGetter = joinWithDot(parameter.getName(), getter.toString(), flatElement.getter());// example parent.getSomething().getChild();
                    return simpleBinding(flatElement.elementInfo(), fullGetter);
                })
                .collect(Collectors.joining("\n"));
    }


    private String parameterPath(String parentName, AttachedElementInfo attachedElementInfo) {
        if (attachedElementInfo.getGetter() == null) {
            throw new IllegalStateException("This field does not have a getter " + attachedElementInfo.getName());
        }
        return appendGetter(parentName, attachedElementInfo.getGetter().getSimpleName().toString());
    }

    private String simpleBinding(ElementInfo parameter, String paramGetter) {
        if (paramGetter == null) {
            throw new IllegalArgumentException("param getter can't be null");
        }
        String finalParamGetter = paramGetter;
        var typeInfo = parameter.getTypeInfo();

        if (typeInfo.asTypeMapper().isDateTime()) {
            finalParamGetter = TRANSFORMERS.get(typeInfo.asTypeMapper()).apply(paramGetter);
        } else if (typeInfo.asTypeMapper().mapToWrapper() == TypeMapper.CHARACTER_WRAPPER) {
            finalParamGetter = charToString(paramGetter);
        }
        return bindParameter(
                typeInfo.asTypeMapper().getJdbcSetterMethod(),
                isPreIncrement ? preIncrementVar(POSITION_VAR) : incrementVar(POSITION_VAR),
                finalParamGetter);
    }

    private String bindParameter(String setter, String position, String parameter) {
        // example: stmt.setString(pos++, parameter1);
        return constructMethod(STATEMENT_VAR, setter, position, parameter).concat(";");
    }

    private List<ElementGetter> flattenComposedElementInfo(
            TypeMirror typeMirror,
            Map<TypeMirror, List<AttachedElementInfo>> nestedElementInfo) {

        List<ElementGetter> result = new ArrayList<>();
        Deque<PendingElement> queue = new ArrayDeque<>();
        queue.add(new PendingElement(typeMirror, ""));

        while (!queue.isEmpty()) {
            PendingElement current = queue.poll();

            if (nestedElementInfo == null || !nestedElementInfo.containsKey(current.typeMirror())) {
                continue;
            }

            for (var elementInfo : nestedElementInfo.get(current.typeMirror())) {
                String currentPath = current.parentPath().isEmpty()
                        ? elementInfo.getGetter().toString()
                        : joinWithDot(current.parentPath(), elementInfo.getGetter().toString());

                if (elementInfo.getTypeInfo().isSimple()) {
                    result.add(new ElementGetter(elementInfo, currentPath));
                } else {
                    queue.add(new PendingElement(elementInfo.getTypeInfo().getMirror(), currentPath));
                }
            }
        }

        return result;
    }
}
