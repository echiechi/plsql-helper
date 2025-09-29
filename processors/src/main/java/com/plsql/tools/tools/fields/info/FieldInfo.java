package com.plsql.tools.tools.fields.info;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Objects;

public class FieldInfo extends VariableInfo {
    private ExecutableElement getter;
    private ExecutableElement setter;

    public FieldInfo(VariableElement clazz) {
        super(clazz);
    }

    public FieldInfo(VariableElement clazz, ExecutableElement getter) {
        this(clazz);
        this.getter = getter;
    }

    public FieldInfo(VariableElement clazz, ExecutableElement getter, ExecutableElement setter) {
        this(clazz, getter);
        this.setter = setter;
    }

    public ExecutableElement getGetter() {
        return getter;
    }

    public void setGetter(ExecutableElement getter) {
        this.getter = getter;
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
        FieldInfo fieldInfo = (FieldInfo) o;
        return Objects.equals(field, fieldInfo.field) && Objects.equals(getter, fieldInfo.getter) && Objects.equals(setter, fieldInfo.setter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, getter, setter);
    }

    @Override
    public String toString() {
        return "FieldInfo{" +
                "field=" + field +
                ", getter=" + getter +
                ", setter=" + setter +
                '}';
    }
}
