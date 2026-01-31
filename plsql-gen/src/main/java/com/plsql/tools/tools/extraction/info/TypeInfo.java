package com.plsql.tools.tools.extraction.info;

import com.plsql.tools.enums.TypeMapper;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

@Data 
public class TypeInfo {
    private TypeMirror mirror;
    private Element rawType;
    private TypeMirror wrappedType;
    private Element rawWrappedType;
    private boolean isRecord;

    public boolean isSimple() {
        return TypeMapper.isSimple(typeAsString());
    }

    public boolean isWrappedSimple() {
        return TypeMapper.isSimple(wrappedTypeAsString());
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
    
    public boolean isWrapped() {
        return wrappedType != null;
    }
    
}
