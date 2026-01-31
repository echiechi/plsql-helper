package com.plsql.tools.tools.extraction.info;

import lombok.Data;

@Data
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
}
