package com.plsql.tools.tools.extraction.info;

import javax.lang.model.element.ExecutableElement;

public class ElementInfo {
    protected TypeInfo typeInfo;
    protected String name;
    protected ExecutableElement defaultConstructor;

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

    public ExecutableElement getDefaultConstructor() {
        return defaultConstructor;
    }

    public void setDefaultConstructor(ExecutableElement defaultConstructor) {
        this.defaultConstructor = defaultConstructor;
    }

    @Override
    public String toString() {
        return "ElementInfo{" +
                "typeInfo=" + typeInfo +
                ", name='" + name + '\'' +
                ", defaultConstructor=" + defaultConstructor +
                '}';
    }
}
