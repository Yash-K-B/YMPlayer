package com.yash.ymplayer.models;

import android.net.Uri;

public class YoutubeSongUriDetail {
    private String id;
    private String lowUri;
    private String mediumUri;
    private String highUri;
    private Long length;

    public YoutubeSongUriDetail(String id, String lowUri, String mediumUri, String highUri, Long length) {
        this.id = id;
        this.lowUri = lowUri;
        this.mediumUri = mediumUri;
        this.highUri = highUri;
        this.length = length;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLowUri() {
        return lowUri;
    }

    public void setLowUri(String lowUri) {
        this.lowUri = lowUri;
    }

    public String getMediumUri() {
        return mediumUri;
    }

    public void setMediumUri(String mediumUri) {
        this.mediumUri = mediumUri;
    }

    public String getHighUri() {
        return highUri;
    }

    public void setHighUri(String highUri) {
        this.highUri = highUri;
    }

    public Long getLength() {
        return length == null ? 0L : length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public Uri getUri(int quality) {
        switch (quality) {
            case 1:
                return parse(lowUri);
            case 2:
                return parse(mediumUri);
            case 3:
                return parse(highUri);
            default:
                return null;
        }
    }

    private Uri parse(String val) {
        if (val == null)
            return null;
        return Uri.parse(val);
    }
}
