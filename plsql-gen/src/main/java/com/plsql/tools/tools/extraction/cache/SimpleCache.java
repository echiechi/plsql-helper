package com.plsql.tools.tools.extraction.cache;

import java.util.Optional;
import java.util.Set;

public interface SimpleCache<K, V> {
    Optional<V> get(K type);

    public void put(K type, V elements);

    public boolean contains(K type);

    public void clear();

    public int size();

    // For debugging
    public Set<String> getCachedTypeNames();
}
