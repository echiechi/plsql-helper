package com.plsql.tools.tools.extraction.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
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
    }

    public void addElement(AttachedElementInfo elementInfo) {
        elementInfoList.add(elementInfo);
    }

    public void addNestedElement(TypeMirror element, List<AttachedElementInfo> elementInfo) {
        nestedElementInfo.put(element, elementInfo);
    }
}
