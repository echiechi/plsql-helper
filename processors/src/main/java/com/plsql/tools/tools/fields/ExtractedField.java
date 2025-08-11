package com.plsql.tools.tools.fields;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Objects;

public class ExtractedField {
    private final String parentObjectName;
    private final Element parameter;
    private ExecutableElement getter;
    private ExecutableElement setter;

    public ExtractedField(String parentObjectName, Element parameter) {
        this.parentObjectName = parentObjectName;
        this.parameter = parameter;
    }

    public ExtractedField(String parentObjectName, Element parameter, ExecutableElement getter) {
        this.parentObjectName = parentObjectName;
        this.parameter = parameter;
        this.getter = getter;
    }

    public ExtractedField(String parentObjectName, Element parameter, ExecutableElement getter, ExecutableElement setter) {
        this.parentObjectName = parentObjectName;
        this.parameter = parameter;
        this.getter = getter;
        this.setter = setter;
    }

    public String getParentObjectName() {
        return parentObjectName;
    }

    public Element getParameter() {
        return parameter;
    }

    public ExecutableElement getGetter() {
        return getter;
    }

    public ExecutableElement getSetter() {
        return setter;
    }
    public void setSetter(ExecutableElement setter) {
        this.setter = setter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedField that = (ExtractedField) o;
        return Objects.equals(parentObjectName, that.parentObjectName) && Objects.equals(parameter.getSimpleName(), that.parameter.getSimpleName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentObjectName, parameter.getSimpleName());
    }

    @Override
    public String toString() {
        return "ExtractedField{" +
                "parentObjectName='" + parentObjectName + '\'' +
                ", parameter=" + parameter +
                ", getter=" + getter +
                ", setter=" + setter +
                '}';
    }
}
