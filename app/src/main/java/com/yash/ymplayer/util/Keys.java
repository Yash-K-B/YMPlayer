package com.yash.ymplayer.util;

public interface Keys {
    String MEDIA_ID = "mediaId";
    String TITLE = "title";
    String ARTIST = "artist";
    String ALBUM = "album";
    String PLAYLIST_NAME = "playlist_name";
    String PLAY_SINGLE = "single";
    String QUEUE_POS = "queuePos";
    String FROM_POSITION = "fromPosition";
    String TO_POSITION = "toPosition";
    String REPEAT_MODE = "repeatMode";
    String SHUFFLE_MODE = "shuffleMode";
    String QUEUE_TYPE = "queueType";
    String ARTWORK = "artwork";
    String EXTRA_PARENT_ID = "parent_id";
    String EXTRA_TYPE = "type";
    String EXTRA_TITLE = "title";
    String EXTRA_ALBUM_ID = "albumId";
    String EXTRA_ARTIST_ID ="artistId";
    String QUEUE_HINT = "queueHint";
    String EXTRA_ART_URL = "EXTRA_ART_URL";

    interface Action {
        String ADD_TO_PLAYLIST = "addToPlaylist";
        String QUEUE_NEXT = "queueNext";
        String PLAY_FROM_QUEUE = "playFromQueue";
        String REMOVE_FROM_QUEUE = "removeFromQueue";
        String SWAP_QUEUE_ITEM = "swapQueueItem";
        String TOGGLE_FAVOURITE = "favourite";
    }

    interface Fragments {
        String LOCAL_SONGS = "localSongs";
        String SETTINGS = "settings";
        String ABOUT = "about";
        String SPOTIFY_SONGS = "spotifySongs";
        String YOUTUBE_SONGS = "youtubeSongs";
    }

    interface SHARED_PREFERENCES {
        String SPOTIFY = "SPOTIFY";
        String AUDIO_URL_MAPPING = "AUDIO_URL_MAPPING";
    }

    interface PREFERENCE_KEYS {
        String TOKEN = "token";
    }

    interface PLAYLISTS {
        String FAVOURITES = "Favourites";
    }

    enum PLAYING_MODE {
        OFFLINE,
        ONLINE
    }

    interface QUEUE_TITLE {
        String CUSTOM = "CUSTOM";
        String USER_QUEUE = "USER";
    }

    interface QueueType {
        String ALL_SONGS = "ALL_SONGS";
        String ARTISTS = "ARTISTS";
        String ALBUMS = "ALBUMS";
    }
}
