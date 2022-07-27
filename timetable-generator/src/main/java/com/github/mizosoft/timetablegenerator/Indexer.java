package com.github.mizosoft.timetablegenerator;

interface Indexer<V> {
    int indexOf(V value);

    V valueOf(int index);

    int size();
  }