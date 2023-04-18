package com.yash.ymplayer.ui.youtube.livepage;

import java.util.List;

import lombok.AllArgsConstructor;

public class PagedResponse<T> {
    private List<T> items;
    private String prevToken;
    private String nextToken;

    public PagedResponse(List<T> items, String prevToken, String nextToken) {
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
}
