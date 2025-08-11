package com.plsql.tools.tools.fields;

import java.util.*;

public class FieldMappingResult {
    private final Set<ExtractedField> fieldMethodMap;
    private final List<String> warnings;
    private final boolean success;
    private final String errorMessage;

    private FieldMappingResult(Set<ExtractedField> fieldMethodMap, List<String> warnings,
                               boolean success, String errorMessage) {
        this.fieldMethodMap = fieldMethodMap != null ? fieldMethodMap : new LinkedHashSet<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static FieldMappingResult success(Set<ExtractedField> fieldMethodMap, List<String> warnings) {
        return new FieldMappingResult(fieldMethodMap, warnings, true, null);
    }

    public static FieldMappingResult failure(String errorMessage) {
        return new FieldMappingResult(null, null, false, errorMessage);
    }

    public Set<ExtractedField> getFieldMethodMap() {
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
