package com.yash.ymplayer.download.manager.constants;

public enum DownloadStatus {
    DOWNLOADED("Downloading"),
    PROCESSING("Processing"),
    FAILED("Failed"),
    PAUSED("Paused"),
    DOWNLOADING("Downloading");

    private final String value;

    DownloadStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
