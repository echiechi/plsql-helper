package com.plsql.tools.tools.extraction.info;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposedElementInfo extends ElementInfo {

    private List<AttachedElementInfo> elementInfoList = new ArrayList<>();
    private Map<TypeMirror, List<AttachedElementInfo>> nestedElementInfo = new HashMap<>();

    public ComposedElementInfo() {
    }

    public ComposedElementInfo(TypeInfo typeInfo, String name) {
        super(typeInfo, name);
    }

    public ComposedElementInfo(TypeInfo typeInfo) {
        setTypeInfo(typeInfo);
        // setName(typeInfo.getRawType().getSimpleName().toString());
    }
    public List<AttachedElementInfo> getElementInfoList() {
        return elementInfoList;
    }

    public Map<TypeMirror, List<AttachedElementInfo>> getNestedElementInfo() {
        return nestedElementInfo;
    }

    public void addElement(AttachedElementInfo elementInfo) {
        elementInfoList.add(elementInfo);
    }

    public void addNestedElement(TypeMirror element, List<AttachedElementInfo> elementInfo) {
        nestedElementInfo.put(element, elementInfo);
    }

    public void setElementInfoList(List<AttachedElementInfo> elementInfoList) {
        this.elementInfoList = elementInfoList;
    }

    public void setNestedElementInfo(Map<TypeMirror, List<AttachedElementInfo>> nestedElementInfo) {
        this.nestedElementInfo = nestedElementInfo;
    }

    @Override
    public String toString() {
        return super.toString() + ", ComposedElementInfo{" +
                "elementInfoList=" + elementInfoList +
                ", nestedElementInfo=" + nestedElementInfo +
                '}';
    }
}
