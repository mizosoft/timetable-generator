package com.github.mizosoft.timetablegenerator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HashIndexer<V> implements Indexer<V> {
  private final int size;
  private final List<V> values;
  private final Map<V, Integer> indices;

  HashIndexer(Collection<V> values) {
    this.values = List.copyOf(values);
    this.size = values.size();

    var v = new HashMap<V, Integer>();
    int i = 0;
    for (var value : values) {
      v.put(value, i++);
    }
    this.indices = Map.copyOf(v);
  }

  @Override
  public int indexOf(V value) {
    return indices.getOrDefault(value, -1);
  }

  @Override
  public V valueOf(int index) {
    return values.get(index);
  }

  @Override
  public int size() {
    return size;
  }
}
