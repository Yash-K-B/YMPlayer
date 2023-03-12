package com.yash.ymplayer.download.manager.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.yash.ymplayer.download.manager.constants.DownloadStatus;

@Entity
public class Download {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String videoId;

    private String fileImageUrl;
    private String fileName;
    private String fileSubText;
    private long fileLength;
    private String uri;
    private int bitrate;
    private String extension;
    private DownloadStatus status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getFileImageUrl() {
        return fileImageUrl;
    }

    public void setFileImageUrl(String fileImageUrl) {
        this.fileImageUrl = fileImageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSubText() {
        return fileSubText;
    }

    public void setFileSubText(String fileSubText) {
        this.fileSubText = fileSubText;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }
}
