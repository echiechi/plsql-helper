package com.plsql.tools.tools.extraction.cache;

import com.plsql.tools.tools.extraction.info.AttachedElementInfo;

import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

public class ExtractionCache implements SimpleCache<TypeMirror, List<AttachedElementInfo>> {
    private final Map<TypeMirror, List<AttachedElementInfo>> elementCache = new HashMap<>();

    @Override
    public Optional<List<AttachedElementInfo>> get(TypeMirror type) {
        return Optional.ofNullable(elementCache.get(type));
    }

    @Override
    public void put(TypeMirror type, List<AttachedElementInfo> elements) {
        elementCache.put(type, elements);
    }

    @Override
    public boolean contains(TypeMirror type) {
        return elementCache.containsKey(type);
    }

    @Override
    public void clear() {
        elementCache.clear();
    }

    @Override
    public int size() {
        return elementCache.size();
    }

    @Override
    public Set<String> getCachedTypeNames() {
        return elementCache.keySet().stream()
                .map(TypeMirror::toString)
                .collect(Collectors.toSet());
    }
}
