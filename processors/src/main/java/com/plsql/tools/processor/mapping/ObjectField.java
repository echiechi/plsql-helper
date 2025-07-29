package com.plsql.tools.processor.mapping;

import javax.lang.model.element.Element;
import java.util.Objects;

public class ObjectField {
    private String objectName;
    private Element field;

    public ObjectField(String objectName, Element field) {
        this.objectName = objectName;
        this.field = field;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Element getField() {
        return field;
    }

    public void setField(Element field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectField that = (ObjectField) o;
        return Objects.equals(objectName, that.objectName) && Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectName, field);
    }
}
