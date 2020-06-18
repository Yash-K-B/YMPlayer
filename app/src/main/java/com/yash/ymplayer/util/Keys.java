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

    interface Action {
        String ADD_TO_PLAYLIST = "addToPlaylist";
        String QUEUE_NEXT = "queueNext";
        String PLAY_FROM_QUEUE = "playFromQueue";
        String REMOVE_FROM_QUEUE = "removeFromQueue";
        String SWAP_QUEUE_ITEM = "swapQueueItem";
        String TOGGLE_FAVOURITE = "favourite";
    }
    interface Fragments{
        String LOCAL_SONGS = "localSongs";
        String SETTINGS = "settings";
        String ABOUT = "about";
        String SPOTIFY_SONGS = "spotifySongs";
    }

    interface SHARED_PREFERENCES {
        String SPOTIFY = "SPOTIFY";
    }
    interface PREFERENCE_KEYS{
        String TOKEN = "token";
    }

    public interface PLAYLISTS {
        String FAVOURITES = "Favourites";
    }
}
