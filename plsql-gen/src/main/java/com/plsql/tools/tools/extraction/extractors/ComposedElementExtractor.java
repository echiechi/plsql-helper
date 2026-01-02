package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class ComposedElementExtractor {

    private final TypeInfoExtractor typeInfoExtractor;
    private final SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    public ComposedElementExtractor(TypeInfoExtractor typeInfoExtractor, SimpleCache<TypeMirror, List<AttachedElementInfo>> cache) {
        this.typeInfoExtractor = typeInfoExtractor;
        this.cache = cache;
    }

    public ComposedElementInfo convertInto(Element record) {
        if (record == null) {
            throw new IllegalArgumentException("@Record class cannot be null");
        }
        var typeInfo = typeInfoExtractor.extractTypeInfo(record);
        return convertInto(record, typeInfo);
    }

    public ComposedElementInfo convertInto(Element record, TypeInfo typeInfo) {
        ComposedElementInfo composedElementInfo = new ComposedElementInfo(typeInfo);
        composedElementInfo.setName(record.getSimpleName().toString());
        if (cache.contains(record.asType())) {
            for (AttachedElementInfo attachedElement : cache.get(record.asType()).orElse(List.of())) {
                composedElementInfo.addElement(attachedElement);
                if (!attachedElement.getTypeInfo().isSimple()) {
                    populateNestedObjects(composedElementInfo, attachedElement.getTypeInfo().getMirror());
                }
            }
        }
        return composedElementInfo;
    }

    private void populateNestedObjects(ComposedElementInfo composedElementInfo, TypeMirror attachedElement) {
        cache.get(attachedElement).ifPresent((nestedAttachedElements) -> {
            composedElementInfo.addNestedElement(attachedElement, nestedAttachedElements);
            for (var element : nestedAttachedElements) {
                if (!element.getTypeInfo().isSimple()) {
                    var type = element.getTypeInfo().getMirror();
                    if (element.getTypeInfo().isWrapped()) {
                        type = element.getTypeInfo().getWrappedType();
                    }
                    populateNestedObjects(composedElementInfo, type);
                }
            }
        });
    }
}
