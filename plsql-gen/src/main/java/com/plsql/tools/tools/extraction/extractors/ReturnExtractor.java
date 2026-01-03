package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.plsql.tools.tools.CodeGenConstants.POSITION_VAR;
import static com.plsql.tools.tools.CodeGenConstants.RETURN_VAR;
import static com.plsql.tools.tools.Tools.isVoid;

public class ReturnExtractor {

    private final TypeInfoExtractor typeInfoExtractor;
    private final ComposedElementExtractor composedElementExtractor;
    private final SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    public ReturnExtractor(TypeInfoExtractor typeInfoExtractor,
                           ComposedElementExtractor composedElementExtractor,
                           SimpleCache<TypeMirror, List<AttachedElementInfo>> cache) {
        this.typeInfoExtractor = typeInfoExtractor;
        this.composedElementExtractor = composedElementExtractor;
        this.cache = cache;
    }

    public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
        var returnType = method.getReturnType();
        if (isVoid(returnType.toString())) {
            return Collections.emptyList();
        }
        Output[] outputs = method.getAnnotation(PlsqlCallable.class).outputs();

        if (outputs.length == 0) {
            throw new IllegalStateException(
                    "Method has a return type but no @Output annotation: " + method.getSimpleName()
            );
        }
        return outputs.length == 1
                ? extractSingleOutput(method, outputs[0])
                : extractMultipleOutputs(method, outputs);
    }

    private List<ReturnElementInfo> extractSingleOutput(
            ExecutableElement method,
            Output output
    ) {
        // check for primitive return value:
        var type = TypeMapper
                .fromSimpleName(getReturnTypeKindName(method));
        DeclaredType returnType;
        if (method.getReturnType() instanceof DeclaredType) {
            returnType = (DeclaredType) method.getReturnType();
        } else if (type != null) {
            returnType = typeInfoExtractor.getDeclaredType(type.mapToWrapper().getDisplayName());
        } else {
            throw new IllegalStateException("return type is not supported " + method.getReturnType());
        }
        return List.of(createReturnElementInfo(returnType, output));
    }

    private static String getReturnTypeKindName(ExecutableElement method) {
        if (method.getReturnType().getKind() == null) {
            return "";
        }
        return method.getReturnType().getKind().toString().toLowerCase();
    }

    private List<ReturnElementInfo> extractMultipleOutputs(
            ExecutableElement method,
            Output[] outputs
    ) {
        DeclaredType returnType = (DeclaredType) method.getReturnType();
        List<AttachedElementInfo> attachedElements = cache.get(returnType.asElement().asType())
                .orElseThrow(() -> new IllegalStateException(
                        "Return type not in cache: " + returnType
                ));

        ElementInfo parentElementInfo = createParentElementInfo(returnType);

        return Arrays.stream(outputs)
                .map(output -> mapOutputToReturnElement(output, attachedElements, parentElementInfo))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private ElementInfo createParentElementInfo(DeclaredType returnType) {
        var typeInfo = typeInfoExtractor.extractTypeInfo(returnType);
        return new ElementInfo(typeInfo, RETURN_VAR);
    }

    private Optional<ReturnElementInfo> mapOutputToReturnElement(
            Output output,
            List<AttachedElementInfo> attachedElements,
            ElementInfo parent
    ) {
        return attachedElements.stream()
                .filter(element -> element.getName().equals(output.field()))
                .findFirst()
                .map(element -> {
                    ReturnElementInfo returnInfo = createReturnElementInfo(element, output);
                    returnInfo.setParent(parent);
                    return returnInfo;
                });
    }

    private ReturnElementInfo createReturnElementInfo(AttachedElementInfo attachedElementInfo, Output output) {
        var typeInfo = attachedElementInfo.getTypeInfo();
        return createReturnElementInfo(attachedElementInfo.getName(), output, typeInfo);
    }

    private ReturnElementInfo createReturnElementInfo(DeclaredType returnType, Output output) {
        TypeInfo typeInfo = typeInfoExtractor.extractTypeInfo(returnType);
        return createReturnElementInfo(RETURN_VAR, output, typeInfo);
    }

    private ReturnElementInfo createReturnElementInfo(
            String defaultName,
            Output output,
            TypeInfo typeInfo) {
        String finalName = output.field().isBlank() ? defaultName : output.field();
        if (typeInfo.isSimple()) {
            return createSimpleReturnElement(typeInfo, finalName, output);
        } else if (typeInfo.isWrapped()) {
            return createWrappedReturnElement(typeInfo, finalName, output);
        } else {
            return createComposedReturnElement(typeInfo, finalName, output);
        }
    }

    private ReturnElementInfo createSimpleReturnElement(
            TypeInfo typeInfo,
            String name,
            Output output
    ) {
        TypeMapper jdbcType = typeInfo.asTypeMapper();
        if (jdbcType == null) {
            throw new IllegalStateException(
                    "Type cannot be mapped to JDBC type: " + typeInfo.typeAsString()
            );
        }
        return new ReturnElementInfo(typeInfo, name, output, POSITION_VAR);
    }

    private ReturnElementInfo createWrappedReturnElement(
            TypeInfo typeInfo,
            String name,
            Output output
    ) {
        Element wrappedElement = typeInfo.getRawWrappedType();
        ComposedElementInfo composedInfo = composedElementExtractor.convertInto(wrappedElement);
        composedInfo.setTypeInfo(typeInfo);
        composedInfo.setName(name);
        return new ReturnElementInfo(composedInfo, output, POSITION_VAR);
    }

    private ReturnElementInfo createComposedReturnElement(
            TypeInfo typeInfo,
            String name,
            Output output
    ) {
        Element element = typeInfo.getRawType();
        ComposedElementInfo composedInfo = composedElementExtractor.convertInto(element, typeInfo);
        composedInfo.setName(name);
        return new ReturnElementInfo(composedInfo, output, POSITION_VAR);
    }
}
