package com.yash.ymplayer.util;

import android.net.Uri;

public class YoutubeSong {
    String title, videoId, channelTitle, channelDesc;
    String art_url_small, art_url_medium;
    Uri audioUri;
    long durationMillis;

    public YoutubeSong(String title, String videoId, String channelTitle, String art_url_small, String art_url_medium) {
        this.title = title;
        this.videoId = videoId;
        this.channelTitle = channelTitle;
        this.art_url_small = art_url_small;
        this.art_url_medium = art_url_medium;
    }

    public String getChannelDesc() {
        return channelDesc;
    }

    public void setChannelDesc(String channelDesc) {
        this.channelDesc = channelDesc;
    }

    public Uri getAudioUri() {
        return audioUri;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getTitle() {
        return title;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public String getArt_url_small() {
        return art_url_small;
    }

    public String getArt_url_medium() {
        return art_url_medium;
    }

    public void setAudioUri(Uri audioUri) {
        this.audioUri = audioUri;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public void setArt_url_small(String art_url_small) {
        this.art_url_small = art_url_small;
    }

    public void setArt_url_medium(String art_url_medium) {
        this.art_url_medium = art_url_medium;
    }

    @Override
    public String toString() {
        return "YoutubeSong{" +
                "videoId='" + videoId + '\'' +
                '}';
    }
}
