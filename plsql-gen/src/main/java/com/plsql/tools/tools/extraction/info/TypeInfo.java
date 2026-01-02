package com.plsql.tools.tools.extraction.info;

import com.plsql.tools.enums.TypeMapper;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

public class TypeInfo {
    private TypeMirror mirror;
    private Element rawType;
    private TypeMirror wrappedType;
    private Element rawWrappedType;

    public boolean isSimple() {
        return TypeMapper.isSimple(typeAsString());
    }

    public boolean isRecord() {
        return rawType.getKind() == ElementKind.RECORD
                || (rawWrappedType != null && rawWrappedType.getKind() == ElementKind.RECORD);
    }

    public TypeMapper asTypeMapper() {
        return TypeMapper.fromSimpleName(typeAsString());
    }

    public TypeMapper wrappedTypeAsTypeMapper() {
        return TypeMapper.fromSimpleName(wrappedTypeAsString());
    }

    public String wrappedTypeAsString() {
        return wrappedType.toString();
    }

    public String typeAsString() {
        return mirror.toString();
    }

    public TypeMirror getMirror() {
        return mirror;
    }

    public void setMirror(TypeMirror mirror) {
        this.mirror = mirror;
    }

    public Element getRawType() {
        return rawType;
    }

    public void setRawType(Element rawType) {
        this.rawType = rawType;
    }

    public TypeMirror getWrappedType() {
        return wrappedType;
    }

    public void setWrappedType(TypeMirror wrappedType) {
        this.wrappedType = wrappedType;
    }

    public boolean isWrapped() {
        return wrappedType != null;
    }

    public Element getRawWrappedType() {
        return rawWrappedType;
    }

    public void setRawWrappedType(Element rawWrappedType) {
        this.rawWrappedType = rawWrappedType;
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "mirror=" + mirror +
                ", rawType=" + rawType +
                ", wrappedType=" + wrappedType +
                ", rawWrappedType=" + rawWrappedType +
                '}';
    }
}
