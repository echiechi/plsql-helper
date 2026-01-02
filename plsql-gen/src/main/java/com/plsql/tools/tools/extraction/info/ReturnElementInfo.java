package com.plsql.tools.tools.extraction.info;

import com.plsql.tools.annotations.Output;

import java.util.ArrayList;
import java.util.List;

public class ReturnElementInfo extends ComposedElementInfo {

    private ElementInfo parent;
    private Output output;
    private String pos;

    public ReturnElementInfo() {
    }

    public ReturnElementInfo(Output output, String pos) {
        this.output = output;
        this.pos = pos;
    }

    public ReturnElementInfo(TypeInfo typeInfo, String name, Output output, String pos) {
        super(typeInfo, name);
        this.output = output;
        this.pos = pos;
    }

    public ReturnElementInfo(ComposedElementInfo composedElementInfo, Output output, String pos) {
        this(composedElementInfo.getTypeInfo(), composedElementInfo.getName(), output, pos);
        super.setElementInfoList(composedElementInfo.getElementInfoList());
        super.setNestedElementInfo(composedElementInfo.getNestedElementInfo());
    }

    public boolean hasRedundantType() {
        List<String> temp = new ArrayList<>();
        var isRedundant = checkAndAlimTemp(getElementInfoList(), temp);
        if (isRedundant) {
            return true;
        }
        for (var elements : getNestedElementInfo().values()) {
            isRedundant = checkAndAlimTemp(elements, temp);
            if (isRedundant) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAndAlimTemp(List<AttachedElementInfo> attachedElementInfoList, List<String> temp) {
        for (var element : attachedElementInfoList) {
            var typeInfo = element.getTypeInfo();
            if (typeInfo.isSimple()) {
                continue;
            }
            var type = typeInfo.typeAsString();
            if (temp.contains(type)) {
                return true;
            } else {
                temp.add(type);
            }
        }
        return false;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public ElementInfo getParent() {
        return parent;
    }

    public void setParent(ElementInfo parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "ReturnElementInfo{" +
                "parent=" + parent +
                ", output=" + output +
                ", pos='" + pos + '\'' +
                ", typeInfo=" + typeInfo +
                ", name='" + name + '\'' +
                '}';
    }
}
