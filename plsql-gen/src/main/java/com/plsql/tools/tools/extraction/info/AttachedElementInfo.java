package com.plsql.tools.tools.extraction.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.ExecutableElement;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttachedElementInfo extends ElementInfo {
    private ExecutableElement getter;
    private ExecutableElement setter;
    private boolean isPublic;
}
