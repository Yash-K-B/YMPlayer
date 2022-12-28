package com.yash.ymplayer.storage;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;

@Entity(indices = {@Index(value = {"mediaId","name","artist","album","playlistId"},unique = true)}, foreignKeys = @ForeignKey(entity = PlayList.class,parentColumns = "id",childColumns = "playlistId"))
public class MediaItem {
    @PrimaryKey(autoGenerate = true)
    int id;

    String mediaId;
    String name;
    String artist;
    String album;
    Integer playlistId;
    String artwork;
    Long timeStamp;

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
}
