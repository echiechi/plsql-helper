package com.plsql.tools.tools.extraction.info;

public class ElementInfo {
    protected TypeInfo typeInfo;
    protected String name;
    protected String alias;

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return "ElementInfo{" +
                "typeInfo=" + typeInfo +
                ", name='" + name + '\'' +
                ", alias='" + alias + '\'' +
                '}';
    }
}
