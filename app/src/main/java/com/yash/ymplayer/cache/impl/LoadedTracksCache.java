package com.yash.ymplayer.cache.impl;

import com.yash.ymplayer.cache.Cache;
import com.yash.ymplayer.ui.youtube.livepage.PagedResponse;
import com.yash.ymplayer.util.StringUtil;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoadedTracksCache implements Cache<String, PagedResponse<YoutubeSong>> {

    private final Map<String, List<YoutubeSong>> loadedTracksMap = new HashMap<>();
    private final Map<String, String> keys = new HashMap<>();
    private final Set<String> loadedKeys = new HashSet<>();

    @Override
    public PagedResponse<YoutubeSong> get(String key) {
        if (!StringUtil.hasText(key))
            return PagedResponse.empty();
        key = key.contains("-") ? key.substring(0, key.indexOf("-")) : key;
        String nextPageToken = keys.get(key);
        List<YoutubeSong> youtubeSongs = loadedTracksMap.get(key);
        return PagedResponse.of(youtubeSongs, nextPageToken);
    }

    @Override
    public boolean contain(String key) {
        return loadedKeys.contains(key);
    }

    @Override
    public void put(String key, PagedResponse<YoutubeSong> value) {
        if (value.getItems() == null || value.getItems().isEmpty())
            return;
        loadedKeys.add(key);
        key = key.contains("-") ? key.substring(0, key.indexOf("-")) : key;
        List<YoutubeSong> youtubeSongs = loadedTracksMap.get(key);
        if (youtubeSongs == null)
            youtubeSongs = new ArrayList<>();
        youtubeSongs.addAll(value.getItems());
        loadedTracksMap.put(key, youtubeSongs);
        keys.put(key, value.getNextToken());
    }

    @Override
    public boolean isEmpty() {
        return loadedTracksMap.isEmpty();
    }

    @Override
    public int size() {
        return loadedTracksMap.size();
    }

    @Override
    public void clear() {
        loadedTracksMap.clear();
        keys.clear();
        loadedKeys.clear();
    }

    @Override
    public PagedResponse<YoutubeSong> remove(String key) {
        loadedKeys.remove(key);
        key = key.contains("-") ? key.substring(0, key.indexOf("-")) : key;
        String nextPageToken = keys.remove(key);
        List<YoutubeSong> youtubeSongs = loadedTracksMap.remove(key);
        return PagedResponse.of(youtubeSongs, nextPageToken);
    }
}
