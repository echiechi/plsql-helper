package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static com.plsql.tools.tools.Tools.extractName;

public class ParameterExtractor {

    private final TypeInfoExtractor typeInfoExtractor;
    private final ComposedElementExtractor composedElementExtractor;

    private final SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    public ParameterExtractor(TypeInfoExtractor typeInfoExtractor,
                              ComposedElementExtractor composedElementExtractor,
                              SimpleCache<TypeMirror, List<AttachedElementInfo>> cache) {
        this.typeInfoExtractor = typeInfoExtractor;
        this.composedElementExtractor = composedElementExtractor;
        this.cache = cache;
    }

    public List<ElementInfo> extractParams(ExecutableElement method) {
        List<ElementInfo> elementInfoList = new ArrayList<>();
        for (var parameter : method.getParameters()) {
            var paramName = parameter.getSimpleName().toString();
            TypeInfo typeInfo = typeInfoExtractor.extractTypeInfo(parameter);
            ElementInfo elementInfo = new ElementInfo(typeInfo, paramName);
            if (typeInfo.isSimple()) {
                elementInfoList.add(elementInfo);
            } else {
                elementInfoList.add(composedElementExtractor.convertInto(parameter));
            }
        }
        return elementInfoList;
    }

    public List<String> extractPramNames(List<ElementInfo> elementInfoList) {
        List<String> paramNames = new ArrayList<>();
        for (var elementInfo : elementInfoList) {
            if (elementInfo.getTypeInfo().isSimple()) {
                paramNames.add(extractName(elementInfo));
            } else {
                paramNames.addAll(extractNestedParamNames(elementInfo.getTypeInfo().getMirror()));
            }
        }
        return paramNames;
    }

    private List<String> extractNestedParamNames(TypeMirror attachedElement) {
        List<String> paramNames = new ArrayList<>();
        cache.get(attachedElement)
                .ifPresent((nestedAttachedElements) -> {
                    for (var innerElement : nestedAttachedElements) {
                        if (innerElement.getTypeInfo().isSimple()) {
                            paramNames.add(extractName(innerElement));
                        } else {
                            var type = innerElement.getTypeInfo().getMirror();
                            if (innerElement.getTypeInfo().isWrapped()) {
                                type = innerElement.getTypeInfo().getWrappedType();
                            }
                            paramNames.addAll(extractNestedParamNames(type));
                        }
                    }
                });
        return paramNames;
    }

}
