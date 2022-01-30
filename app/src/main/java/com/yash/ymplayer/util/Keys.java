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
    String VIDEO_ID = "videoId";
    String AUDIO_SESSION_ID = "audioSessionId";
    String EXTRA_EQUALIZER_STATE = "equalizerState";
    String EXTRA_PLAYBACK_QUALITY = "playbackQuality";
    String EXTRA_LENGTH = "length";
    String EXTRA_DOWNLOAD_QUALITY = "downloadQuality";

    interface Action {
        String ADD_TO_PLAYLIST = "addToPlaylist";
        String QUEUE_NEXT = "queueNext";
        String PLAY_FROM_QUEUE = "playFromQueue";
        String REMOVE_FROM_QUEUE = "removeFromQueue";
        String SWAP_QUEUE_ITEM = "swapQueueItem";
        String TOGGLE_FAVOURITE = "favourite";
        String DOWNLOAD = "download";
        String AUDIO_SESSION_CHANGED = "audioSessionChanged";
        String TOGGLE_EQUALIZER_STATE = "toggleEqualizerState";
        String PLAYBACK_QUALITY_CHANGED = "qualityChanged";
        String CLOSE_PLAYBACK = "closePlayback";
    }

    interface Fragments {
        String LOCAL_SONGS = "localSongs";
        String SETTINGS = "settings";
        String ABOUT = "about";
        String YOUTUBE_SONGS = "youtubeSongs";
        String DOWNLOADS = "downloads";
    }

    interface SHARED_PREFERENCES {
        String DOWNLOADS = "downloads";
    }

    interface PREFERENCE_KEYS {
        String TOKEN = "token";
        String BUILTIN_EQUALIZER = "builtin_equalizer";
        String DOWNLOADS = "downloads";
        String TOTAL_DOWNLOADS = "totalDownloads";
        String PLAYBACK_QUALITY = "playback_quality";
        String IS_EXCEPTION = "isException";
        String EXCEPTION = "exception";
        String LOUDNESS_GAIN = "loudness_gain";
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

    interface COMMAND {
        String AUDIO_SESSION_ID = "audioSessionId";
        String GET_AUDIO_SESSION_ID = "getAudioSessionId";
        String UPDATE_EQUALIZER = "updateEqualizer";
        String ON_AUDIO_SESSION_ID_CHANGE = "onAudioSessionIdChange";
    }

    interface OBJECT {
        String EQUALIZER = "equalizer";
        String BASSBOOST = "bassBoost";
        String PRESETREVERB = "presetReverb";
    }

    interface DownloadManager {
        String EXTRA_ACTION = "action";
        String EXTRA_VIDEO_ID = "videoId";
        String EXTRA_TASK_ID = "taskId";
        String EXTRA_BITRATE = "bitrate";
    }

    interface Notification{
        String CHANNEL_ID = "YMNotification";
        CharSequence CHANNEL_NAME = "YM Notification";
    }
}
