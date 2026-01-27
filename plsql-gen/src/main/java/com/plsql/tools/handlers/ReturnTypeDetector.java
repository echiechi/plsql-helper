package com.plsql.tools.handlers;

import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;

public class ReturnTypeDetector {
    private final Extractor extractor;

    public ReturnTypeDetector(Extractor extractor) {
        this.extractor = extractor;
    }

    public ReturnCategory categorize(ReturnElementInfo element) {
        TypeInfo typeInfo = element.getTypeInfo();

        if (typeInfo.isSimple()) {
            return ReturnCategory.SIMPLE;
        } else if (!typeInfo.isWrapped()) {
            return ReturnCategory.COMPOSED;
        } else if (isOptionalSimple(typeInfo)) {
            return ReturnCategory.OPTIONAL_SIMPLE;
        } else if (isOptionalComposed(typeInfo)) {
            return ReturnCategory.OPTIONAL_COMPOSED;
        } else if (isCollection(typeInfo)) {
            return ReturnCategory.COLLECTION;
        }

        throw new IllegalStateException("Unknown return type: " + typeInfo);
    }

    public enum ReturnCategory {
        SIMPLE, COMPOSED, OPTIONAL_SIMPLE, OPTIONAL_COMPOSED, COLLECTION
    }

    private boolean isOptionalSimple(TypeInfo typeInfo) {
        return typeInfo.isWrapped() && typeInfo.isWrappedSimple()
                && extractor.isOptional(typeInfo.getMirror());
    }

    private boolean isOptionalComposed(TypeInfo typeInfo) {
        return typeInfo.isWrapped() &&
                !typeInfo.isWrappedSimple()
                && extractor.isOptional(typeInfo.getMirror());
    }

    private boolean isCollection(TypeInfo typeInfo) {
        return typeInfo.isWrapped() &&
                extractor.isCollection(typeInfo.getMirror());
    }

}
