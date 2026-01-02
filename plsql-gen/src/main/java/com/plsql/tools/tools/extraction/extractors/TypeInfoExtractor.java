package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.info.TypeInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.Optional;

public class TypeInfoExtractor {
    private final Types typeUtils;

    private final Elements elementsUtils;

    public TypeInfoExtractor(ProcessingContext context) {
        this.typeUtils = context.getProcessingEnv().getTypeUtils();
        this.elementsUtils = context.getProcessingEnv().getElementUtils();
    }

    public TypeInfo extractTypeInfo(Element field) {
        var typeInfo = new TypeInfo();
        typeInfo.setMirror(field.asType());
        typeInfo.setRawType(field);
        if (field.asType().getKind() == TypeKind.DECLARED) {
            extractWrappedType(field.asType()).ifPresent(t -> {
                typeInfo.setWrappedType(t);
                typeInfo.setRawWrappedType(typeUtils.asElement(t));
            });
        }
        return typeInfo;
    }

    public TypeInfo extractTypeInfo(DeclaredType field) {
        var typeInfo = new TypeInfo();
        typeInfo.setMirror(field);
        typeInfo.setRawType(field.asElement());
        if (!field.getTypeArguments().isEmpty()) {
            extractWrappedType(field).ifPresent(t -> {
                typeInfo.setWrappedType(t);
                typeInfo.setRawWrappedType(typeUtils.asElement(t));
            });
        }
        return typeInfo;
    }

    public boolean isCollection(TypeMirror type) {
        return isAssignableFrom(type, Collection.class.getCanonicalName());
    }

    public boolean isOptional(TypeMirror type) {
        return isAssignableFrom(type, Optional.class.getCanonicalName());
    }

    private boolean isAssignableFrom(TypeMirror type, String baseTypeName) {
        TypeElement baseElement = elementsUtils.getTypeElement(baseTypeName);
        if (baseElement == null) {
            return false;
        }
        TypeMirror baseType = baseElement.asType();
        TypeMirror erasedType = typeUtils.erasure(type);
        TypeMirror erasedBase = typeUtils.erasure(baseType);
        return typeUtils.isAssignable(erasedType, erasedBase);
    }

    private Optional<? extends javax.lang.model.type.TypeMirror> extractWrappedType(TypeMirror asType) {
        return ((DeclaredType) asType).getTypeArguments()
                .stream()
                .findFirst();
    }
}
