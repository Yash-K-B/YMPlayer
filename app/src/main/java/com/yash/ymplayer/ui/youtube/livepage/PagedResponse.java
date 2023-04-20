package com.yash.ymplayer.ui.youtube.livepage;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;

public class PagedResponse<T> {
    private final List<T> items;
    private final String prevToken;
    private final String nextToken;

    private PagedResponse(List<T> items, String prevToken, String nextToken) {
        this.items = items;
        this.prevToken = prevToken;
        this.nextToken = nextToken;
    }

    public List<T> getItems() {
        return items;
    }

    public String getPrevToken() {
        return prevToken;
    }

    public String getNextToken() {
        return nextToken;
    }

    public static <T> PagedResponse<T> of(List<T> items, String prevToken, String nextToken) {
        return new PagedResponse<>(items, prevToken, nextToken);
    }

    public static <T> PagedResponse<T> of(List<T> items, String nextToken) {
        return PagedResponse.of(items, null, nextToken);
    }

    public static <T> PagedResponse<T> of(List<T> items) {
        return PagedResponse.of(items, null, null);
    }

    public static <T> PagedResponse<T> empty() {
        return new PagedResponse<>(new ArrayList<>(), null, null);
    }
}
