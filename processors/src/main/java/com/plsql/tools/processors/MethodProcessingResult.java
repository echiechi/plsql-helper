package com.plsql.tools.processors;

import com.plsql.tools.tools.fields.ExtractedField;

import javax.lang.model.type.DeclaredType;
import java.util.*;

public class MethodProcessingResult {
    public static class ParameterResult {
        private final List<String> parameters;
        private final Set<ExtractedField> extractedFields;
        private boolean isValid = true;

        public ParameterResult(List<String> parameters, Set<ExtractedField> extractedFields) {
            this.parameters = parameters;
            this.extractedFields = extractedFields;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public Set<ExtractedField> getExtractedFields() {
            return extractedFields;
        }

        public void setValid(boolean valid) {
            isValid = valid;
        }

        @Override
        public String toString() {
            return "ParameterResult{" +
                    "parameters=" + parameters +
                    ", extractedFields=" + extractedFields +
                    ", isValid=" + isValid +
                    '}';
        }
    }

    public static class ReturnResult {
        private final Map<DeclaredType, Set<ExtractedField>> extractedReturnFields;
        private boolean isValid = true;

        public ReturnResult() {
            this.extractedReturnFields = new LinkedHashMap<>();
        }

        public void addType(DeclaredType type) {
            extractedReturnFields.put(type, new LinkedHashSet<>());
        }
        public Set<ExtractedField> getByType(DeclaredType type) {
            return extractedReturnFields.get(type);
        }
        
        public void setValid(boolean valid) {
            isValid = valid;
        }

        public Map<DeclaredType, Set<ExtractedField>> getExtractedReturnFields() {
            return extractedReturnFields;
        }
        @Override
        public String toString() {
            return "ReturnResult{" +
                    "extractedReturnFields=" + extractedReturnFields +
                    ", isValid=" + isValid +
                    '}';
        }
    }

    private final ParameterResult parameterResult;
    private final ReturnResult returnResult;

    public MethodProcessingResult(ParameterResult parameterResult, ReturnResult returnResult) {
        this.parameterResult = parameterResult;
        this.returnResult = returnResult;
    }

    public List<String> getParameters() {
        return parameterResult.getParameters();
    }

    public Set<ExtractedField> getMethodParameters() {
        return parameterResult.getExtractedFields();
    }

    public Map<DeclaredType, Set<ExtractedField>> getMethodReturns() {
        return returnResult.extractedReturnFields;
    }

    public ReturnResult getReturnResult() {
        return returnResult;
    }

    public boolean isValid() {
        return returnResult.isValid && parameterResult.isValid;
    }
}
