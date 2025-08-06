package com.plsql.tools.tools.fields;

import com.plsql.tools.mapping.ObjectField;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FieldMappingResult {
    private final Map<ObjectField, Element> fieldMethodMap;
    private final List<String> warnings;
    private final boolean success;
    private final String errorMessage;

    private FieldMappingResult(Map<ObjectField, Element> fieldMethodMap, List<String> warnings,
                               boolean success, String errorMessage) {
        this.fieldMethodMap = fieldMethodMap != null ? fieldMethodMap : new LinkedHashMap<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static FieldMappingResult success(Map<ObjectField, Element> fieldMethodMap, List<String> warnings) {
        return new FieldMappingResult(fieldMethodMap, warnings, true, null);
    }

    public static FieldMappingResult failure(String errorMessage) {
        return new FieldMappingResult(null, null, false, errorMessage);
    }

    public Map<ObjectField, Element> getFieldMethodMap() {
        return fieldMethodMap;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
