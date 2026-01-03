package com.plsql.tools.tools.extraction;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.extractors.*;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class Extractor {
    private final ProcessingContext context;
    private final TypeInfoExtractor typeInfoExtractor;
    private final ClassInfoExtractor classInfoExtractor;
    private final ComposedElementExtractor composedElementExtractor;
    private final ParameterExtractor parameterExtractor;
    private final ReturnExtractor returnExtractor;

    public Extractor(ProcessingContext context) {
        if (context == null) {
            throw new IllegalStateException("Context is not yet defined");
        }
        this.context = context;
        typeInfoExtractor = new TypeInfoExtractor(context);
        classInfoExtractor = new ClassInfoExtractor(typeInfoExtractor, context);
        composedElementExtractor = new ComposedElementExtractor(typeInfoExtractor, context.getCache());
        parameterExtractor = new ParameterExtractor(typeInfoExtractor, composedElementExtractor, context.getCache());
        returnExtractor = new ReturnExtractor(typeInfoExtractor, composedElementExtractor, context.getCache());
    }

    public ProcessingContext getContext() {
        return context;
    }

    public List<AttachedElementInfo> getAttachedElements(TypeMirror typeMirror) {
        return context.getCache().get(typeMirror).orElse(List.of());
    }

    public boolean isCollection(TypeMirror type) {
        return typeInfoExtractor.isCollection(type);
    }

    public boolean isOptional(TypeMirror type) {
        return typeInfoExtractor.isOptional(type);
    }

    public void extractClassInfoAndAlimCache(Element classOrRecord) {
        context.getCache().get(
                        classOrRecord.asType())
                .orElseGet(() -> {
                    var toReturn = classInfoExtractor.extractClassInfo(classOrRecord);
                    context.getCache().put(classOrRecord.asType(), toReturn);
                    return toReturn;
                });
    }

    public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
        return returnExtractor.extractReturn(method);
    }

    public List<String> extractPramNames(List<ElementInfo> elementInfoList) {
        return parameterExtractor.extractPramNames(elementInfoList);
    }

    public List<ElementInfo> extractParams(ExecutableElement method) {
        return parameterExtractor.extractParams(method);
    }

    public ComposedElementInfo convertInto(Element record) {
        var typeInfo = typeInfoExtractor.extractTypeInfo(record);
        return composedElementExtractor.convertInto(record, typeInfo);
    }

    public TypeMirror eraseType(TypeMirror typeMirror) {
        return typeInfoExtractor.eraseType(typeMirror);
    }
}
