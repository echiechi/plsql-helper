package com.plsql.tools.tools.extraction.info;

import com.plsql.tools.enums.JdbcHelper;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

public class TypeInfo {
    private TypeMirror mirror;
    private Element rawType;
    private TypeMirror wrappedType;
    private Element rawWrappedType;

    public boolean isRecord() {
        return rawType.getKind() == ElementKind.RECORD
                || (rawWrappedType != null && rawWrappedType.getKind() == ElementKind.RECORD);
    }

    public static boolean isSimple(TypeMirror type) {
        var typeAsStr = type.toString();
        return type.getKind().isPrimitive() ||
                typeAsStr.startsWith("java.lang.")
                || typeAsStr.startsWith("java.time.")
                || typeAsStr.equals("java.util.Date")
                || typeAsStr.equals("java.math.BigDecimal")
                || typeAsStr.equals("java.math.BigInteger");
    }

    public JdbcHelper asJdbcHelper() {
        return JdbcHelper.fromSimpleName(typeAsString());
    }

    public JdbcHelper wrappedTypeAsJdbcHelper() {
        return JdbcHelper.fromSimpleName(wrappedTypeAsString());
    }

    public String wrappedTypeAsString() {
        return wrappedType.toString();
    }

    public String typeAsString() {
        return mirror.toString();
    }

    public boolean isSimple() {
        var type = typeAsString();
        return mirror.getKind().isPrimitive() ||
                type.startsWith("java.lang.")
                || type.startsWith("java.time.")
                || type.equals("java.util.Date")
                || type.equals("java.math.BigDecimal")
                || type.equals("java.math.BigInteger");
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
