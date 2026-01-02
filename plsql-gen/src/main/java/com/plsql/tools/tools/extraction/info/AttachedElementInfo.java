package com.plsql.tools.tools.extraction.info;

import javax.lang.model.element.ExecutableElement;

public class AttachedElementInfo extends ElementInfo {
    private ExecutableElement getter;
    private ExecutableElement setter;
    private boolean isPublic;

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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @Override
    public String toString() {
        return super.toString() + ", AttachedElementInfo{" +
                "getter=" + getter +
                ", setter=" + setter +
                ", isPublic=" + isPublic +
                '}';
    }
}
