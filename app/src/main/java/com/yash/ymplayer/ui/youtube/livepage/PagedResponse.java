package com.yash.ymplayer.ui.youtube.livepage;

import com.yash.ymplayer.util.YoutubeSong;
import com.yash.youtube_extractor.models.YoutubeResponse;

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

    public static PagedResponse<YoutubeSong> from(YoutubeResponse youtubeResponse) {
        List<YoutubeSong> songs = new ArrayList<>();
        for (com.yash.youtube_extractor.models.YoutubeSong youtubeSong : youtubeResponse.getSongs()) {
            YoutubeSong song = new YoutubeSong(youtubeSong.getTitle(), youtubeSong.getVideoId(), youtubeSong.getChannelTitle(), youtubeSong.getArtUrlSmall(), youtubeSong.getArtUrlMedium(), youtubeSong.getArtUrlHigh());
            song.setDurationMillis(youtubeSong.getDurationMillis());
            songs.add(song);
        }
        return PagedResponse.of(songs, youtubeResponse.getContinuationToken());
    }
}
