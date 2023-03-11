package com.yash.ymplayer.ffmpeg;

import lombok.Builder;
import lombok.Getter;

public class AudioMetadata {
    private final String title;
    private final String album;
    private final String artist;
    private final String year;
    private final String composer;
    private final String genre;
    private final String albumArt;

    private AudioMetadata(String title, String album, String artist, String year, String composer, String genre, String albumArt) {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.year = year;
        this.composer = composer;
        this.genre = genre;
        this.albumArt = albumArt;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public String getComposer() {
        return composer;
    }

    public String getGenre() {
        return genre;
    }

    public String getAlbumArt() {
        return albumArt;
    }


    public static final class Builder {

        private String title;
        private String album;
        private String artist;
        private String year;
        private String composer;
        private String genre;
        private String albumArt;

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withAlbum(String album) {
            this.album = album;
            return this;
        }

        public Builder withArtist(String artist) {
            this.artist = artist;
            return this;
        }

        public Builder withYear(String year) {
            this.year = year;
            return this;
        }

        public Builder withComposer(String composer) {
            this.composer = composer;
            return this;
        }

        public Builder withGenre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder withAlbumArt(String albumArt) {
            this.albumArt = albumArt;
            return this;
        }

        public AudioMetadata build() {
            return new AudioMetadata(title, album, artist, year, composer, genre, albumArt);
        }
    }
}
