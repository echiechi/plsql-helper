package com.plsql.tools.processors;

import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import java.util.List;

@Deprecated
public class MethodProcessingResult {
    private final List<VariableInfo> parameterInfo;
    private final VariableInfo returnResult;
    private final List<String> parameterNames;

    public MethodProcessingResult(List<VariableInfo> parameterInfo,
                                  VariableInfo returnResult,
                                  List<String> parameterNames) {
        this.parameterInfo = parameterInfo;
        this.returnResult = returnResult;
        this.parameterNames = parameterNames;
    }

    public List<VariableInfo> getParameterInfo() {
        return parameterInfo;
    }

    public VariableInfo getReturnResult() {
        return returnResult;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

}
