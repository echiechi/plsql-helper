package com.plsql.tools.tools.fields.info;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.*;

@Deprecated
public class ObjectInfo extends VariableInfo {
    private final String objectName;
    private Set<FieldInfo> fieldInfoSet;
    private final Map<Element, Set<FieldInfo>> nestedObjects = new LinkedHashMap<>();
    private TypeElement wrapper;

    public ObjectInfo(String objectName, Element clazz) {
        super(clazz);
        this.objectName = Objects.requireNonNullElse(objectName, "");
    }

    public String getObjectName() {
        return objectName;
    }

    public Set<FieldInfo> getFieldInfoSet() {
        return fieldInfoSet;
    }

    public void setFieldInfoSet(Set<FieldInfo> fieldInfoSet) {
        this.fieldInfoSet = fieldInfoSet;
    }

    public void addFieldInfo(FieldInfo fieldInfo) {
        if (fieldInfoSet == null) {
            fieldInfoSet = new LinkedHashSet<>();
        }
        this.fieldInfoSet.add(fieldInfo);
    }

    public Map<Element, Set<FieldInfo>> getNestedObjects() {
        return nestedObjects;
    }

    public void addToNestedObjects(Element element, Set<FieldInfo> fieldInfoSet) {
        nestedObjects.put(element, fieldInfoSet);
    }

    public TypeElement getWrapper() {
        return wrapper;
    }

    public void setWrapper(TypeElement wrapper) {
        this.wrapper = wrapper;
    }

    public String getWrappedUpName() {
        return String.format("%sWrapped", getObjectName());
    }

    @Override
    public boolean isWrapped() {
        return wrapper != null;
    }

    @Override
    public String toString() {
        return "ObjectInfo{" +
                "objectName='" + objectName + '\'' +
                ", fieldInfoSet=" + fieldInfoSet +
                ", nestedObjects=" + nestedObjects +
                ", wrapper=" + wrapper +
                '}';
    }
}
