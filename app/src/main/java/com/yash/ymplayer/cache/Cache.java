package com.yash.ymplayer.cache;

import java.util.Optional;

public interface Cache<K, V> {
    V get(K key);

    boolean contain(K key);

    void put(K key, V value);

    boolean isEmpty();

    int size();

    void clear();
}
