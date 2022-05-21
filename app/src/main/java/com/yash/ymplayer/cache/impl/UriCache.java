package com.yash.ymplayer.cache.impl;

import android.net.Uri;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.cache.Cache;
import com.yash.ymplayer.models.YoutubeSongUriDetail;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UriCache implements Cache<String, YoutubeSongUriDetail> {
    private static final String TAG = "UriCache";

    private final LinkedHashMap<String, YoutubeSongUriDetail> cache;
    int capacity;
    private final ReentrantReadWriteLock lock;

    public UriCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity);
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public YoutubeSongUriDetail get(String key) {
        lock.writeLock().lock();
        try {
            LogHelper.d(TAG, "get: Accessed key: " + key);
            YoutubeSongUriDetail uriDetail = cache.get(key);
            cache.remove(key);
            cache.put(key, uriDetail);
            return uriDetail;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean contain(String key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(String key, YoutubeSongUriDetail value) {
        this.lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                // Key already exist
                cache.remove(key);
                cache.put(key, value);
            } else {
                // Key not present, checking for capacity
                if (cache.size() == capacity) {
                    // cache is full
                    String firstKey = cache.keySet().iterator().next();
                    cache.remove(firstKey);
                }
                cache.put(key, value);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return cache.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        this.lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
