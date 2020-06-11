package com.yash.ymplayer.util;

public class Keys {
    public static final String MEDIA_ID = "mediaId";
    public static final String TITLE = "title";
    public static final String ARTIST = "artist";
    public static final String ALBUM = "album";
    public static final String PLAYLIST_NAME = "playlist_name";
    public static final String PLAY_SINGLE = "single";
    public static final String QUEUE_POS = "queuePos";
    public static final String FROM_POSITION = "fromPosition";
    public static final String TO_POSITION = "toPosition";

    public static class Action {
        public static final String ADD_TO_PLAYLIST = "addToPlaylist";
        public static final String QUEUE_NEXT = "queueNext";
        public static final String PLAY_FROM_QUEUE = "playFromQueue";
        public static final String REMOVE_FROM_QUEUE = "removeFromQueue";
        public static final String SWAP_QUEUE_ITEM = "swapQueueItem";
        public static final String TOGGLE_FAVOURITE = "favourite";
    }
    public static class Fragments{
        public static final String LOCAL_SONGS = "localSongs";
        public static final String SETTINGS = "settings";
        public static final String ABOUT = "about";
    }
}
