package com.plsql.tools.processors.params;

import com.plsql.tools.mapping.ObjectField;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;

public class ProcessingResult {
    private final List<String> parameters;
    private final Map<ObjectField, Element> extractedMap;
    private final boolean valid;

    public ProcessingResult(List<String> parameters, Map<ObjectField, Element> extractedMap, boolean valid) {
        this.parameters = parameters;
        this.extractedMap = extractedMap;
        this.valid = valid;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Map<ObjectField, Element> getExtractedMap() {
        return extractedMap;
    }

    public boolean isValid() {
        return valid;
    }
}
