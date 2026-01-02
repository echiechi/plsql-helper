package com.plsql.tools.tools.extraction.info;

public class ElementInfo {
    protected TypeInfo typeInfo;
    protected String name;

    public ElementInfo() {
    }

    public ElementInfo(TypeInfo typeInfo, String name) {
        this.typeInfo = typeInfo;
        this.name = name;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ElementInfo{" +
                "typeInfo=" + typeInfo +
                ", name='" + name + '\'' +
                '}';
    }
}
