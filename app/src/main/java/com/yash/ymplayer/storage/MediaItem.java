package com.yash.ymplayer.storage;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;

@Entity(indices = {@Index(value = {"mediaId","name","artist","album","playlistId"},unique = true)}, foreignKeys = @ForeignKey(entity = PlayList.class,parentColumns = "id",childColumns = "playlistId"))
public class MediaItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String mediaId;
    private String name;
    private String artist;
    private String album;
    private Integer playlistId;
    private String artwork;
    private Long timeStamp;

    public MediaItem(String mediaId, String name, String artist, String album, Integer playlistId, String artwork) {
        this.mediaId = mediaId;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.playlistId = playlistId;
        this.artwork = artwork;
        this.timeStamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public String getMediaId() {
        return mediaId;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Integer getPlaylistId() {
        return playlistId;
    }

    public String getArtwork() {
        return artwork;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setPlaylistId(Integer playlistId) {
        this.playlistId = playlistId;
    }

    public void setArtwork(String artwork) {
        this.artwork = artwork;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
