package com.plsql.tools.handlers;

import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

public interface ReturnTypeHandler {
    boolean canHandle(ReturnElementInfo returnElement);

    String generateCode(ReturnElementInfo returnElement);
}
