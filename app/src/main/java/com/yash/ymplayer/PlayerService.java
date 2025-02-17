package com.yash.ymplayer;

import static android.net.ConnectivityManager.NetworkCallback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.session.PlaybackState;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.ResolvingDataSource;
import com.google.android.exoplayer2.util.Util;
import com.yash.logging.LogHelper;
import com.yash.logging.utils.ExceptionUtil;
import com.yash.ymplayer.cache.impl.UriCache;
import com.yash.ymplayer.constant.Constants;
import com.yash.ymplayer.interfaces.PlayerHelper;
import com.yash.ymplayer.models.YoutubeSongUriDetail;
import com.yash.ymplayer.pool.ThreadPool;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.interfaces.AudioProvider;
import com.yash.ymplayer.storage.MediaItem;
import com.yash.ymplayer.storage.PlayList;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.EqualizerUtil;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.MediaItemHelperUtility;
import com.yash.ymplayer.util.PlayerHelperUtil;
import com.yash.ymplayer.util.Song;
import com.yash.ymplayer.util.YoutubeSong;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.StreamingData;
import com.yash.youtube_extractor.models.VideoDetails;
import com.yash.youtube_extractor.models.YoutubePlaylist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerService extends MediaBrowserServiceCompat implements PlayerHelper {
    public static final String STATE_PREF = "PlayerState";
    public static final String METADATA_KEY_LIKE = "like";
    public static final String MEDIA_ID = "mediaId";
    private static final String TAG = "PlayerService";
    public static final String METADATA_KEY_FAVOURITE = "favourite";
    private static final int NOTIFICATION_ID = 10;
    MediaSessionCompat mSession;
    PlaybackStateCompat.Builder mPlaybackStateBuilder;
    MediaMetadataCompat.Builder mMediaMetadataBuilder;
    String currentMediaIdOrVideoId;
    SimpleExoPlayer player;
    List<MediaSessionCompat.QueueItem> playingQueue;
    List<String> mediaIdLists;
    int queuePos = -1;
    List<Song> songs;
    SharedPreferences preferences;
    long likeState = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    AudioManager audioManager;
    AudioFocusRequest audioFocusRequest;
    private long savedPlayerPosition;
    ConcatenatingMediaSource mediaSources;
    int repeatMode;
    boolean isShuffleModeEnabled;
    boolean isSeek;
    Keys.PLAYING_MODE playingMode = Keys.PLAYING_MODE.OFFLINE;
    SharedPreferences appPreferences;
    ConnectivityManager connectivityManager;
    boolean isInternetAvailable;
    PlaybackEndedStatus playbackEndedStatus;
    DataSource.Factory factory;
    ExtractorsFactory extractorsFactory;
    boolean isQueueChanged;
    UriCache uriCache;

    List<ResultReceiver> resultReceivers;

    private int audioSessionId;

    private NotificationManager notificationManager;
    private boolean isForegroundService;

    //Youtube Extractor
    Extractor extractor;

    private String queueTitleContext;

    /* Declares that ContentStyle is supported */
    public static final String CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED";

    /*
     * Bundle extra indicating the presentation hint for playable media items.
     */
    public static final String CONTENT_STYLE_PLAYABLE_HINT =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";

    /*
     * Bundle extra indicating the presentation hint for browsable media items.
     */
    public static final String CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";

    /*
     * Specifies the corresponding items should be presented as lists.
     */
    public static final int CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1;

    /*
     * Specifies that the corresponding items should be presented as grids.
     */
    public static final int CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2;

    /*
     * Specifies that the corresponding items should be presented as lists and are
     * represented by a vector icon. This adds a small margin around the icons
     * instead of filling the full available area.
     */
    public static final int CONTENT_STYLE_CATEGORY_LIST_ITEM_HINT_VALUE = 3;

    /*
     * Specifies that the corresponding items should be presented as grids and are
     * represented by a vector icon. This adds a small margin around the icons
     * instead of filling the full available area.
     */
    public static final int CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE = 4;


    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate: Service");

        //Variables
        songs = new ArrayList<>();
        playingQueue = new ArrayList<>();
        preferences = getSharedPreferences(STATE_PREF, MODE_PRIVATE);
        appPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mediaSources = new ConcatenatingMediaSource();
        mediaIdLists = new ArrayList<>();
        resultReceivers = new ArrayList<>();
        isSeek = false;
        repeatMode = preferences.getInt(Keys.REPEAT_MODE, 0);
        isShuffleModeEnabled = preferences.getBoolean(Keys.SHUFFLE_MODE, false);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //Youtube Extractor
        extractor = new Extractor();

        //MediaSession
        mSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
        mSession.setCallback(mediaSessionCallbacks);
        setSessionToken(mSession.getSessionToken());
        initPlaybackState();
        mSession.setRepeatMode(repeatMode);
        mSession.setShuffleMode(isShuffleModeEnabled ? PlaybackStateCompat.SHUFFLE_MODE_ALL : PlaybackStateCompat.SHUFFLE_MODE_NONE);
        mSession.setActive(true);


        //MediaPlayer
        player = null;

        initDataSourceFactory();
        uriCache = new UriCache(10);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.d(TAG, "onStartCommand: Handle Event");
        MediaButtonReceiver.handleIntent(mSession, intent);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Bundle extras = new Bundle();
        extras.putBoolean(BrowserRoot.EXTRA_OFFLINE, true);
        return new BrowserRoot("ROOT", extras);
    }


    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        //result.detach();

        List<MediaBrowserCompat.MediaItem> mediaItems = null;
        if (parentId.equals("ROOT")) {
            mediaItems = getRootChildren();
            result.sendResult(mediaItems);
        } else {
            onLoadChildren(parentId, result, new Bundle());
        }
    }


    Result<List<MediaBrowserCompat.MediaItem>> resultSender;

    boolean isResultSent;

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
        LogHelper.d(TAG, "onLoadChildren: PlayerService: Parent Id: " + parentId);
        result.detach();
        resultSender = result;
        ThreadPool.getInstance().getExecutor().execute(() -> processResult(parentId, result));
    }

    private void processResult(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = null;
        if (parentId.contains("YOUTUBE")) {
            loadChannelYoutube(parentId, result);
        } else {
            if (parentId.equals("ALL_SONGS")) {
                mediaItems = Repository.getInstance(this).getAllSongs();
            } else if (parentId.contains("ARTISTS")) {
                if (parentId.equals("ARTISTS"))
                    mediaItems = Repository.getInstance(this).getAllArtists();
                else {
                    mediaItems = Repository.getInstance(this).getSongsOfArtist(parentId);
                }
            } else if (parentId.contains("ALBUMS")) {
                if (parentId.equals("ALBUMS"))
                    mediaItems = Repository.getInstance(this).getAllAlbums();
                else {
                    mediaItems = Repository.getInstance(this).getSongsOfAlbum(parentId);
                }
            } else if (parentId.contains("PLAYLISTS") || parentId.contains(Keys.PlaylistType.HYBRID_PLAYLIST.name())) {
                if (parentId.equals("PLAYLISTS")) {
                    mediaItems = Repository.getInstance(this).getAllPlaylists();
                } else {
                    mediaItems = Repository.getInstance(this).getAllSongsOfPlaylist(parentId);
                }
            }
            LogHelper.d(TAG, "onLoadChildren: PlayerService : MediaItem length - " + ((mediaItems != null) ? mediaItems.size() : null));
            result.sendResult(mediaItems);
        }
    }

    private void loadChannelYoutube(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (parentId.equals("YOUTUBE")) {
            OnlineYoutubeRepository.getInstance(this).getChannelPlaylists(Constants.DEFAULT_CHANNEL, new OnlineYoutubeRepository.PlaylistLoadedCallback() {
                @Override
                public void onLoaded(Map<String, List<YoutubePlaylist>> playlistsByCategory) {
                    List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
                    for (String channel : playlistsByCategory.keySet()) {
                        mediaItems.add(MediaItemHelperUtility.toMediaItems(channel, parentId));
                    }
                    result.sendResult(mediaItems);
                }

                @Override
                public <E extends Exception> void onError(E e) {
                    result.sendResult(new ArrayList<>());
                }
            });
        } else {
            String[] splits = parentId.split("[/|]");
            if (splits.length == 2) {
                OnlineYoutubeRepository.getInstance(this).getChannelPlaylists(Constants.DEFAULT_CHANNEL, new OnlineYoutubeRepository.PlaylistLoadedCallback() {
                    @Override
                    public void onLoaded(Map<String, List<YoutubePlaylist>> playlistsByCategory) {
                        List<YoutubePlaylist> youtubePlaylists = playlistsByCategory.get(splits[1]);
                        youtubePlaylists = youtubePlaylists != null ? youtubePlaylists : new ArrayList<>();
                        result.sendResult(MediaItemHelperUtility.toMediaItems(youtubePlaylists, parentId));
                    }

                    @Override
                    public <E extends Exception> void onError(E e) {
                        result.sendResult(new ArrayList<>());
                    }
                });
            } else if (splits.length > 2) {
                OnlineYoutubeRepository.getInstance(this).getPlaylistTracks(splits[2], "", new OnlineYoutubeRepository.TracksLoadedCallback() {
                    @Override
                    public void onLoaded(List<YoutubeSong> songs) {
                        String[] parents = parentId.split("[/]");
                        result.sendResult(MediaItemHelperUtility.mapToMediaItems(songs, parents[parents.length - 1]));
                    }

                    @Override
                    public <E extends Exception> void onError(E e) {
                        result.sendResult(new ArrayList<>());
                    }
                });
            }
        }
    }


    private List<MediaBrowserCompat.MediaItem> getRootChildren() {
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        Bundle extras = new Bundle();
        items.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId("ALL_SONGS")
                .setTitle("All Tracks")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        items.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setTitle("Artists")
                .setMediaId("ARTISTS")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE);
        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE);
        items.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setTitle("Albums")
                .setMediaId("ALBUMS")
                .setExtras(extras)
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        items.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setTitle("Playlists")
                .setMediaId("PLAYLISTS")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE);
        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE);
        items.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setTitle("Youtube Songs")
                .setMediaId("YOUTUBE")
                .setExtras(extras)
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        return items;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null)
            connectivityManager.unregisterNetworkCallback(networkCallback);
        if (player != null)
            player.release();
        mSession.release();
        removeAsForegroundService(true);
        EqualizerUtil.getInstance(this).release();
        LogHelper.d(TAG, "onDestroy: Service Destroyed");
    }

    MediaSessionCompat.Callback mediaSessionCallbacks = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            LogHelper.d(TAG, "onMediaButtonEvent: " + mediaButtonIntent);
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        /**
         * Override to handle requests to prepare playback. During the preparation, a session should
         * not hold audio focus in order to allow other sessions play seamlessly. The state of
         * playback should be updated to {@link PlaybackState#STATE_PAUSED} after the preparation is
         * done.
         */
        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            LogHelper.d(TAG, "onPlayFromUri: " + uri);
            playingMode = Keys.PLAYING_MODE.ONLINE;
            currentMediaIdOrVideoId = uri.toString();
            setPlayingQueueFromUri(uri, extras);
            resolveQueuePosition(extractId(currentMediaIdOrVideoId));
            dispatchPlayRequest();
        }

        public void onPlayFromYoutube(String videoId, Bundle extras) {
            playingMode = Keys.PLAYING_MODE.ONLINE;
            currentMediaIdOrVideoId = videoId;
            if (PlayerHelperUtil.isSharedYoutube(videoId)) {
                prepareForSharedPlayingQueue(videoId, () -> {
                    resolveQueuePosition(extractId(currentMediaIdOrVideoId));
                    dispatchPlayRequest();
                });
            } else {
                setOnlinePlayingQueue(currentMediaIdOrVideoId, extras);
                resolveQueuePosition(extractId(currentMediaIdOrVideoId));
                dispatchPlayRequest();
            }
        }

        /**
         * Override to handle requests to play a specific mediaId that was
         * provided by your app's {@link MediaBrowserService}.
         *
         * @param mediaId id of the audio
         * @param extras any extra
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            LogHelper.d(TAG, "onPlayFromMediaId: extra: " + extras + " mediaId: " + mediaId);
            String id = extractId(mediaId);
            if (!mediaIdPattern.matcher(id).matches()) {
                onPlayFromYoutube(mediaId, extras);
                return;
            }
            playingMode = Keys.PLAYING_MODE.OFFLINE;
            currentMediaIdOrVideoId = mediaId;
            setPlayingQueue(mediaId, extras);
            resolveQueuePosition(id);
            dispatchPlayRequest();
        }

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            LogHelper.d(TAG, "onPlay: currentMediaId:" + currentMediaIdOrVideoId);

            LogHelper.d(TAG, "onPlay: playbackstate:" + mSession.getController().getPlaybackState().getState());
            if (currentMediaIdOrVideoId == null) {
                if (playingQueue.isEmpty())
                    setPlayingQueue(null, new Bundle());
                queuePos = 0;
            }
            dispatchPlayRequest();
        }


        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            LogHelper.d(TAG, "onPause: ");
            if (player != null) {
                LogHelper.d(TAG, "onPause: isPlaying: " + player.isPlaying());
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                player.setPlayWhenReady(false);
                handler.removeCallbacks(playNextOnMediaError);
                pushNotification(PlaybackStateCompat.STATE_PAUSED);

            }


        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        @Override
        public void onSkipToNext() {
            LogHelper.d(TAG, "onSkipToNext: =================================>");
            handler.removeCallbacks(playNextOnMediaError);

            if (player == null) {
                if (playbackEndedStatus == PlaybackEndedStatus.FINISHED && !isShuffleModeEnabled && repeatMode == ExoPlayer.REPEAT_MODE_OFF)
                    return;
                player = getSimpleExoPlayer(null);
                preparePlayer(player);
                LogHelper.d(TAG, "onSkipToNext: Next window index: " + player.getNextWindowIndex() + " Previous window index:" + player.getPreviousWindowIndex() + " queue pos: " + queuePos + " player current index:" + player.getCurrentWindowIndex() + "Timeline:" + player.getCurrentTimeline().isEmpty() + "    nnnkn:  ");
            } else if (player.getPlaybackError() != null) {
                LogHelper.d(TAG, "onSkipToNext: Playback Error");
                if (player.getNextWindowIndex() == C.INDEX_UNSET) return;
                queuePos = player.getNextWindowIndex();
                preparePlayer(player);
            } else {
                player.next();
            }
            setPlayWhenReady(true);
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        @Override
        public void onSkipToPrevious() {
            LogHelper.d(TAG, "onSkipToPrevious: <==============================================");
            handler.removeCallbacks(playNextOnMediaError);

            if (player == null) {
                player = getSimpleExoPlayer(null);
                preparePlayer(player);
            } else if (player.getPlaybackError() != null) {
                if (player.getPreviousWindowIndex() == C.INDEX_UNSET) return;
                queuePos = player.getPreviousWindowIndex();
                preparePlayer(player);
            } else {
                LogHelper.d(TAG, "onSkipToPrevious: " + player);
                if (player.getContentPosition() > 2000) {
                    isSeek = true;
                    player.seekTo(0);
                } else {
                    LogHelper.d(TAG, "onSkipToPrevious: previous index: " + player.getPreviousWindowIndex() + " playwhenready:" + player.getPlayWhenReady());
                    player.previous();
                }
            }
            setPlayWhenReady(true);
            LogHelper.d(TAG, "onSkipToPrevious: setting playWhenReady true");
        }

        /**
         * Override to handle requests to stop playback.
         */
        @Override
        public void onStop() {
            LogHelper.d(TAG, "onStop: ");
            queuePos = -1;
            playingQueue.clear();
            mediaIdLists.clear();
            mediaSources.clear();
            mSession.setMetadata(null);
            mSession.setQueueTitle("");
            currentMediaIdOrVideoId = null;
            if (player != null) {
                savedPlayerPosition = player.getCurrentPosition();
                player.release();
                player = null;
            }
            setPlaybackState(PlaybackStateCompat.STATE_NONE);
            removeAsForegroundService(true);
            EqualizerUtil.getInstance(PlayerService.this).release();
            LogHelper.d(TAG, "onStop: " + savedPlayerPosition);
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         *
         * @param pos New position to move to, in milliseconds.
         */
        @Override
        public void onSeekTo(long pos) {
            LogHelper.d(TAG, "onSeekTo: " + pos);
            if (player != null) {
                isSeek = true;
                player.seekTo(pos);
                setPlaybackState(mSession.getController().getPlaybackState().getState());
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            LogHelper.d(TAG, "onSetRepeatMode: ");
            PlayerService.this.repeatMode = Math.min(repeatMode, 2);
            if (player != null)
                player.setRepeatMode(PlayerService.this.repeatMode);
            mSession.setRepeatMode(repeatMode);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Keys.REPEAT_MODE, PlayerService.this.repeatMode);
            editor.apply();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            LogHelper.d(TAG, "onSetShuffleMode: ");
            if (shuffleMode == 0) {
                isShuffleModeEnabled = false;
            } else if (shuffleMode > 0) {
                isShuffleModeEnabled = true;
            }
            if (player != null)
                player.setShuffleModeEnabled(isShuffleModeEnabled);
            mSession.setShuffleMode(shuffleMode);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Keys.SHUFFLE_MODE, isShuffleModeEnabled);
            editor.apply();

        }

        @Override
        public void onSkipToQueueItem(long id) {
            LogHelper.d(TAG, "onSkipToQueueItem: id: " + id);
            if (id == -1) return;
            queuePos = (int) id;
            isQueueChanged = false;
            setPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM);
            dispatchPlayRequest();
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            switch (command) {
                case Keys.COMMAND.AUDIO_SESSION_ID:
                    resultReceivers.add(cb);
                    if (player != null) {
                        Bundle extra = new Bundle();
                        extra.putInt(Keys.AUDIO_SESSION_ID, player.getAudioSessionId());
                        cb.send(1001, extra);
                    }
                    break;
                case Keys.COMMAND.GET_AUDIO_SESSION_ID:
                    Bundle extra = new Bundle();
                    int audioSessionId = player == null ? audioManager.generateAudioSessionId() : PlayerService.this.audioSessionId;
                    LogHelper.d(TAG, "onCommand: Generated or Retrieved audioSessionId : " + audioSessionId);
                    extra.putInt(Keys.AUDIO_SESSION_ID, audioSessionId);
                    cb.send(1001, extra);
                    break;
                case Keys.COMMAND.ON_AUDIO_SESSION_ID_CHANGE:
                    resultReceivers.add(cb);
                    break;
                default:
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            LogHelper.d(TAG, "onCustomAction: ");
            Bundle extra = new Bundle();
            switch (action) {
                case Keys.Action.LIKE:
                    likeState = extras.getLong("like_enabled", 0);
                    LogHelper.d(TAG, "onCustomAction: likeState: " + likeState);
                    setMediaMetadata(playingQueue.get(queuePos));
                    break;
                case Keys.Action.ADD_TO_PLAYLIST:
                    if (extras != null && extras.containsKey(Keys.PLAYLIST_NAME) && extras.containsKey(Keys.MEDIA_ID) && extras.containsKey(Keys.TITLE) && extras.containsKey(Keys.ARTIST) && extras.containsKey(Keys.ALBUM)) {
                        String playlist = extras.getString(Keys.PLAYLIST_NAME);
                        String mediaId = extras.getString(Keys.MEDIA_ID);
                        String title = extras.getString(Keys.TITLE);
                        String artist = extras.getString(Keys.ARTIST);
                        String album = extras.getString(Keys.ALBUM);
                        String artwork = extras.getString(Keys.ARTWORK);
                        PlayList playlistObj = Repository.getInstance(PlayerService.this).getPlaylist(playlist);
                        MediaItem item = new MediaItem(mediaId, title, artist, album, playlistObj.getId(), artwork);

                        if (Repository.getInstance(PlayerService.this).addToPlaylist(item) == -1)
                            Toast.makeText(PlayerService.this, "Already Added to " + playlist, Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(PlayerService.this, "Added to " + playlist, Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(PlayerService.this, "Please Select Playlist name", Toast.LENGTH_SHORT).show();
                    break;
                case Keys.Action.QUEUE_NEXT:
                    if (extras != null && extras.containsKey(Keys.MEDIA_ID) && extras.containsKey(Keys.QUEUE_HINT) && extras.containsKey(Keys.QUEUE_MODE)) {
                        String mediaId = extras.getString(Keys.MEDIA_ID);
                        int hint = extras.getInt(Keys.QUEUE_HINT);
                        Keys.QueueMode queueMode = Keys.QueueMode.fromString(extras.getString(Keys.QUEUE_MODE));
                        LogHelper.d(TAG, "onCustomAction: QUEUE_NEXT [Media Id : %s, Hint : %s, Queue Mode: %s]", mediaId, hint, queueMode);

                        if (queueMode == Keys.QueueMode.ONLINE) {
                            LogHelper.d(TAG, "ONLINE QUEUE MODE: fetching queue items from youtube cache ");
                            List<MediaSessionCompat.QueueItem> items = OnlineYoutubeRepository.getInstance(PlayerService.this).getQueue(hint, mediaId);
                            playingQueue.addAll((queuePos + 1), items);
                            for (int i = 0; i < items.size(); i++) {
                                int queue_pos = queuePos + 1 + i;
                                String media_id = items.get(i).getDescription().getMediaId();
                                addHttpSourceToMediaSources(media_id, queue_pos);
                                mediaIdLists.add(queue_pos, media_id);
                            }

                        } else {
                            LogHelper.d(TAG, "OFFLINE QUEUE MODE: fetching queue items from device storage ");
                            List<MediaSessionCompat.QueueItem> items = Repository.getInstance(PlayerService.this).getQueue(hint, mediaId);
                            playingQueue.addAll((queuePos + 1), items);
                            for (int i = 0; i < items.size(); i++) {
                                int queue_pos = queuePos + 1 + i;
                                String media_id = items.get(i).getDescription().getMediaId();
                                addToMediaSources(media_id, queue_pos);
                                mediaIdLists.add(queue_pos, media_id);
                            }
                        }
                        mSession.setQueueTitle(Keys.QUEUE_TITLE.USER_QUEUE);
                        mSession.setQueue(playingQueue);
                        if (player != null)
                            setPlaybackState(mSession.getController().getPlaybackState().getState());
                    }
                    break;

                case Keys.Action.QUEUE_LAST:
                    if (extras != null && extras.containsKey(Keys.MEDIA_ID) && extras.containsKey(Keys.QUEUE_HINT) && extras.containsKey(Keys.QUEUE_MODE)) {
                        String mediaId = extras.getString(Keys.MEDIA_ID);
                        int hint = extras.getInt(Keys.QUEUE_HINT);
                        Keys.QueueMode queueMode = Keys.QueueMode.fromString(extras.getString(Keys.QUEUE_MODE));
                        LogHelper.d(TAG, "onCustomAction: QUEUE_LAST [Media Id : %s, Hint : %s, Queue Mode: %s]", mediaId, hint, queueMode);

                        if (queueMode == Keys.QueueMode.ONLINE) {
                            LogHelper.d(TAG, "ONLINE QUEUE MODE: fetching queue items from youtube cache ");
                            List<MediaSessionCompat.QueueItem> items = OnlineYoutubeRepository.getInstance(PlayerService.this).getQueue(hint, mediaId);
                            int numItems = mediaIdLists.size();
                            playingQueue.addAll(items);
                            for (int i = 0; i < items.size(); i++) {
                                String media_id = items.get(i).getDescription().getMediaId();
                                addHttpSourceToMediaSources(media_id, -1);
                                mediaIdLists.add(media_id);
                            }

                        } else {
                            LogHelper.d(TAG, "OFFLINE QUEUE MODE: fetching queue items from device storage ");
                            List<MediaSessionCompat.QueueItem> items = Repository.getInstance(PlayerService.this).getQueue(hint, mediaId);
                            int numItems = mediaIdLists.size();
                            playingQueue.addAll(items);
                            for (int i = 0; i < items.size(); i++) {
                                String media_id = items.get(i).getDescription().getMediaId();
                                addToMediaSources(media_id, -1);
                                mediaIdLists.add(media_id);
                            }
                        }
                        mSession.setQueueTitle(Keys.QUEUE_TITLE.USER_QUEUE);
                        mSession.setQueue(playingQueue);
                        if (player != null)
                            setPlaybackState(mSession.getController().getPlaybackState().getState());
                    }
                    break;

                case Keys.Action.REMOVE_FROM_QUEUE:
                    if (extras != null && extras.containsKey(Keys.MEDIA_ID)) {
                        int pos = mediaIdLists.indexOf(extras.getString(Keys.MEDIA_ID));
                        if (pos == -1) return;
                        LogHelper.d(TAG, "onCustomAction: REMOVE_FROM_QUEUE:" + " Position found:" + pos);
                        playingQueue.remove(pos);
                        mediaIdLists.remove(pos);
                        mediaSources.removeMediaSource(pos);

                        if (!playingQueue.isEmpty()) {
                            mSession.setQueueTitle(Keys.QUEUE_TITLE.CUSTOM);
                            if (queuePos == pos) {
                                if (isShuffleEnabled()) {
                                    if (player == null) return;
                                    player.setPlayWhenReady(false);
                                    pos = player.getCurrentTimeline().getNextWindowIndex(queuePos, Player.REPEAT_MODE_ALL, true);
                                    player.prepare(mediaSources);
                                    queuePos = pos > queuePos ? pos - 1 : pos;
                                } else {
                                    queuePos = queuePos % playingQueue.size();
                                    LogHelper.d(TAG, "onCustomAction: REMOVE_FROM_QUEUE: queuePos:" + queuePos);
                                }
                                player.seekToDefaultPosition(queuePos);
                                player.setPlayWhenReady(true);
                            } else {
                                if (pos < queuePos) queuePos--;
                                setPlaybackState(mSession.getController().getPlaybackState().getState());
                            }
                            mSession.setQueue(playingQueue);
                        } else {
                            queuePos = -1;
                            mSession.setMetadata(null);
                            mSession.setQueueTitle("");
                            currentMediaIdOrVideoId = null;
                            setPlaybackState(PlaybackStateCompat.STATE_NONE);
                            removeAsForegroundService(true);
                            mSession.getController().getTransportControls().stop();
                        }

                    }
                    break;

                case Keys.Action.SWAP_QUEUE_ITEM:
                    if (extras != null && extras.containsKey(Keys.FROM_POSITION) && extras.containsKey(Keys.TO_POSITION)) {
                        mSession.setQueueTitle(Keys.QUEUE_TITLE.CUSTOM);
                        String mediaId = mediaIdLists.get(queuePos);
                        LogHelper.d(TAG, "onCustomAction: SWAP_QUEUE_ITEM mediaid = " + mediaId + " pos:" + queuePos);
                        int fromPosition = extras.getInt(Keys.FROM_POSITION);
                        int toPosition = extras.getInt(Keys.TO_POSITION);
                        LogHelper.d(TAG, "onCustomAction: from :" + fromPosition + " to :" + toPosition);
                        MediaSessionCompat.QueueItem item = playingQueue.remove(fromPosition);
                        playingQueue.add(toPosition, item);
                        String id = mediaIdLists.remove(fromPosition);
                        mediaIdLists.add(toPosition, id);
                        mediaSources.moveMediaSource(fromPosition, toPosition);
                        queuePos = mediaIdLists.indexOf(mediaId);
                        LogHelper.d(TAG, "onCustomAction: new pos = " + queuePos);
                        setPlaybackState(mSession.getController().getPlaybackState().getState());
                        mSession.setQueue(playingQueue);
                    }
                    break;

                case Keys.Action.TOGGLE_FAVOURITE:
                    MediaDescriptionCompat description = playingQueue.get(queuePos).getDescription();
                    if (description == null || ConverterUtil.toString(description.getMediaId()) == null) {
                        Toast.makeText(PlayerService.this, "Something went wrong!!!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String mediaId = extractId(ConverterUtil.toString(description.getMediaId()));
                    String artwork = mediaIdPattern.matcher(mediaId).matches() ? null : String.valueOf(description.getIconUri());
                    if (mSession.getController().getMetadata().getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0) {
                        Integer playListId = Repository.getInstance(PlayerService.this).getPlaylist(Keys.PLAYLISTS.FAVOURITES).getId();
                        Repository.getInstance(PlayerService.this).addToPlaylist(new MediaItem(mediaId, ConverterUtil.toString(description.getTitle()), ConverterUtil.toString(description.getSubtitle()), ConverterUtil.toString(description.getDescription()), playListId, artwork));
                    } else {
                        Repository.getInstance(PlayerService.this).removeFromPlaylist(mediaId, Keys.PLAYLISTS.FAVOURITES);
                    }

                    setMediaMetadata(playingQueue.get(queuePos));
                    break;


                case Keys.COMMAND.UPDATE_EQUALIZER:
                    LogHelper.d(TAG, "onCustomAction: Updating Equalizer");
                    if (player != null)
                        loadEqualizer(audioSessionId);
                    break;

                case Keys.Action.PLAYBACK_QUALITY_CHANGED:
                    if (player == null) return;
                    String tag = (String) player.getCurrentTag();
                    if (tag != null && mediaIdPattern.matcher(tag).matches()) return;
                    onPause();
                    player = getSimpleExoPlayer(player);
                    seekPlayer(queuePos, savedPlayerPosition);
                    setPlayWhenReady(true);
                    LogHelper.d(TAG, "onCustomAction: PLAYBACK_QUALITY_CHANGED");
                    break;

                case Keys.Action.CLOSE_PLAYBACK:
                    onStop();
                    break;
                default:
            }


        }

    };

    /*
      Custom Methods
     */

    /**
     *
     */
    void dispatchPlayRequest() {
        PlaybackStateCompat mState = mSession.getController().getPlaybackState();
        switch (mState.getState()) {
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                LogHelper.d(TAG, "dispatchPlayRequest: STATE_SKIPPING_TO_QUEUE_ITEM");
                player = getSimpleExoPlayer(player);
                if (player.getPlaybackError() != null) {
                    handler.removeCallbacks(playNextOnMediaError);
                    preparePlayer(player);
                } else seekPlayer(queuePos, C.TIME_UNSET);
                setPlayWhenReady(true);
                break;

            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_PLAYING:
                LogHelper.d(TAG, "dispatchPlayRequest: STATE_BUFFERING or STATE_PLAYING");
                seekPlayer(queuePos, C.TIME_UNSET);
                setPlayWhenReady(true);
                break;

            case PlaybackStateCompat.STATE_PAUSED:
                LogHelper.d(TAG, "dispatchPlayRequest: STATE_PAUSED");
                if (player != null && queuePos != player.getCurrentWindowIndex())
                    player.seekToDefaultPosition(queuePos);
                setPlayWhenReady(true);
                break;

            case PlaybackStateCompat.STATE_STOPPED:
                LogHelper.d(TAG, "dispatchPlayRequest: STATE_STOPPED");
                player = getSimpleExoPlayer(player);
                if (playbackEndedStatus == PlaybackEndedStatus.INTERRUPTED)
                    seekPlayer(queuePos, savedPlayerPosition);
                else seekPlayer(queuePos, 0);
                setPlayWhenReady(true);
                break;
            case PlaybackStateCompat.STATE_NONE:
                LogHelper.d(TAG, "dispatchPlayRequest: STATE_NONE qPos:" + queuePos);
                player = getSimpleExoPlayer(player);
                player.seekTo(queuePos, 0);
                setPlayWhenReady(true);
                break;
            default:
        }
    }


    public void prepareForSharedPlayingQueue(String uri, Runnable runnable) {
        Toast.makeText(PlayerService.this, "Fetching song details", Toast.LENGTH_SHORT).show();
        OnlineYoutubeRepository.getInstance(this).extractSharedSongQueue(uri, new OnlineYoutubeRepository.QueueLoadedCallback() {
            @Override
            public void onLoaded(List<MediaSessionCompat.QueueItem> queueItems) {
                setQueueTitle(uri, true);
                clearMediaSourceAndQueue();
                playingQueue = queueItems;
                prepareMediaSource();
                runnable.run();
                appendWatchNextQueue(uri, null);
            }

            @Override
            public void additionalDetail(VideoDetails videoDetails) {
                LogHelper.d(TAG, "additionalDetail: ");
                updateUriInCacheAndGet(extractId(uri), 0, videoDetails);
            }

            @Override
            public <E extends Exception> void onError(E e) {
                Toast.makeText(PlayerService.this, "Unable to play the song", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String watchNextQueueUri;
    private String watchNextContinuationToken;

    private void appendWatchNextQueue(String uri, String continuationToken) {
        OnlineYoutubeRepository.getInstance(PlayerService.this).extractSharedWatchNextQueue(uri, continuationToken, queueTitleContext, (queueItems, nextToken, tag) -> {
            if (!Objects.equals(tag, queueTitleContext)) {
                LogHelper.d(TAG, "appendWatchNextQueue: Skipping as queue title not matched");
                return;
            }
            buildAndAppendQueue(uri, queueItems);
            watchNextContinuationToken = nextToken;
            watchNextQueueUri = nextToken == null ? null : uri;

        });
    }

    private void buildAndAppendQueue(String uri, List<MediaSessionCompat.QueueItem> queueItems) {
        playingQueue.addAll(queueItems);
        mSession.setQueue(playingQueue);
        for (MediaSessionCompat.QueueItem item : queueItems) {
            String id = item.getDescription().getMediaId();
            mediaIdLists.add(id);
            addHttpSourceToMediaSources(id, -1);
        }
        setQueueTitle(uri, false);
    }


    @Override
    public void setOnlinePlayingQueue(String uri, Bundle extras) {
        if (extras == null)
            extras = new Bundle();
        boolean playSingle = extras.getBoolean(Keys.PLAY_SINGLE, false);
        setQueueTitle(uri, playSingle);

        clearMediaSourceAndQueue();

        if (PlayerHelperUtil.isSearchedYoutube(uri)) {
            playingQueue = MediaItemHelperUtility.getQueueFrom(extras);
            handler.postDelayed(() -> appendWatchNextQueue(uri, null), 100);
        } else {
            if (!playSingle) {
                playingQueue = uri.startsWith(Constants.PREFIX_PLAYLISTS) || uri.startsWith(Keys.PlaylistType.HYBRID_PLAYLIST.name()) ? Repository.getInstance(this).getCurrentPlayingQueue(uri) : OnlineYoutubeRepository.getInstance(this).getPlayingQueue(uri);
            } else {
                playingQueue = uri.startsWith(Constants.PREFIX_PLAYLISTS) || uri.startsWith(Keys.PlaylistType.HYBRID_PLAYLIST.name()) ? Repository.getInstance(this).getCurrentPlayingQueue(uri) : OnlineYoutubeRepository.getInstance(this).getPlayingQueueSingle(uri);
            }
        }

        prepareMediaSource();
    }

    private void clearMediaSourceAndQueue() {
        playingQueue.clear();
        mediaIdLists.clear();
        if (player != null && player.isPlaying())
            player.stop();
        mediaSources.clear();
    }

    private void prepareMediaSource() {
        for (int i = 0; i < playingQueue.size(); i++) {
            String id = playingQueue.get(i).getDescription().getMediaId();
            mediaIdLists.add(id);
            addHttpSourceToMediaSources(id, i);
        }
        mSession.setQueue(playingQueue);
        isQueueChanged = true;
        if (player != null) {
            player.setShuffleModeEnabled(isShuffleEnabled());
            player.prepare(mediaSources);
        }
    }

    private void setQueueTitle(String uri, boolean playSingle) {
        String queueTitle;
        if (playSingle)
            queueTitle = uri + "_PlaYSinglE";
        else queueTitle = uri.split("[|]")[0];
        if (mSession.getController().getQueueTitle() != null && queueTitle.contentEquals(mSession.getController().getQueueTitle())) {
            LogHelper.d(TAG, "setPlayingQueue: .... Queue Already set");
            isQueueChanged = false;
            return;
        }
        mSession.setQueueTitle(queueTitle);
        queueTitleContext = queueTitle;
    }

    @Override
    public int getPositionInQueue(@NonNull String mediaId) {
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        return mediaIdLists.indexOf(id);
    }

    /**
     * Extract the position of mediaId from Queue
     *
     * @param mediaId the id of current media
     */
    @Override
    public void resolveQueuePosition(String mediaId) {
        int pos = mediaIdLists.indexOf(mediaId);
        if (pos != -1) {
            queuePos = pos;
            LogHelper.d(TAG, "resolveQueuePosition: pos:" + queuePos);
        } else {
            if (!playingQueue.isEmpty()) {
                queuePos = 0;
            }
            LogHelper.d(TAG, "mediaId is not available in Queue");
        }
    }

    public String extractId(String mediaId) {
        String[] parts = mediaId.split("[/|]");
        return parts[parts.length - 1];
    }


    /**
     * Obtain Playing Queue and Set to mediaSession
     * The Queue includes - MediaSources, Playing QueueItem
     *
     * @param mediaId the id of current media
     */
    @Override
    public void setPlayingQueue(@Nullable String mediaId, Bundle extras) {
        try {
            boolean playSingle = extras.getBoolean(Keys.PLAY_SINGLE, false);
            setQueueTitle(mediaId, playSingle);

            clearMediaSourceAndQueue();

            if (!playSingle) {
                playingQueue = Repository.getInstance(PlayerService.this).getCurrentPlayingQueue(mediaId);
                LogHelper.d(TAG, "setPlayingQueue: All");
            } else if (extras.getBoolean(Keys.PLAY_SINGLE)) {
                playingQueue = Repository.getInstance(PlayerService.this).getQueue(AudioProvider.QueueHint.SINGLE_SONG, mediaId);
                LogHelper.d(TAG, "setPlayingQueue: Single");
            }
        } catch (NullPointerException e) {
            LogHelper.d(TAG, e.getMessage());
            if (mediaId == null) {
                mSession.setQueueTitle("RANDOM");
                setPlaybackState(PlaybackStateCompat.STATE_NONE);
                playingQueue = Repository.getInstance(PlayerService.this).getRandomQueue();
            }
        }

        prepareMediaSource();

    }

    /**
     * Initial Builder of PlaybackState
     */
    void initPlaybackState() {
        mPlaybackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_PLAY_FROM_URI
                        | PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)
                .setState(PlaybackStateCompat.STATE_NONE, player != null ? player.getCurrentPosition() : 0, player != null ? player.getPlaybackParameters().speed : 1.0f)
                .setBufferedPosition(player != null ? player.getBufferedPosition() : 0)
                .setActiveQueueItemId(queuePos);
        mSession.setPlaybackState(mPlaybackStateBuilder.build());

    }

    /**
     * Build and set playback state to @link MediaSessionCompat
     *
     * @param state current playbackState
     */
    void setPlaybackState(int state) {
        mPlaybackStateBuilder.setState(state, player != null ? player.getCurrentPosition() : 0, player != null ? player.getPlaybackParameters().speed : 1.0f)
                .setBufferedPosition(player != null ? player.getBufferedPosition() : 0)
                .setActiveQueueItemId(queuePos);
        mSession.setPlaybackState(mPlaybackStateBuilder.build());
    }


    int retryCount = 0;

    void loadAlbumArtAndPushNotification(Uri uri) {
        Glide.with(PlayerService.this).asBitmap().load(uri).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                currentBitmapUriPair = new Pair<>(resource, uri.toString());
                setMediaMetadata(playingQueue.get(queuePos));
                pushNotification(PlaybackStateCompat.STATE_PLAYING);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                if (isInternetAvailable && retryCount < 3) {
                    retryCount++;
                    LogHelper.d(TAG, "onLoadFailed: Retry to load album art: retry:" + retryCount);
                    loadAlbumArtAndPushNotification(uri);
                } else {
                    currentBitmapUriPair = new Pair<>(BitmapFactory.decodeResource(getResources(), R.drawable.album_art_placeholder), "");
                    pushNotification(mSession.getController().getPlaybackState().getState());
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    Pair<Bitmap, String> currentBitmapUriPair = new Pair<>(null, "");

    /**
     * Build and set mediaMetaData to MediaSessionCompat
     *
     * @param currentItem current mediaItem playing
     */
    void setMediaMetadata(MediaSessionCompat.QueueItem currentItem) {
        Uri albumArtUri;
        try {
            long playingMediaDuration = 0L;

            addToLastPlayed(currentItem);
            boolean isMediaIdPatterMatch = mediaIdPattern.matcher(currentItem.getDescription().getMediaId()).matches();

            if (isMediaIdPatterMatch || contentUriPattern.matcher(currentItem.getDescription().getMediaId()).matches()) {
                albumArtUri = isMediaIdPatterMatch ? ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(currentItem.getDescription().getMediaId())) : Uri.parse(currentItem.getDescription().getMediaId());
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, albumArtUri);
                byte[] data = retriever.getEmbeddedPicture();
                Bitmap bitmap;
                if (data != null)
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                else
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.album_art_placeholder);
                playingMediaDuration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                currentBitmapUriPair = new Pair<>(bitmap, albumArtUri.toString());
            } else {
                albumArtUri = currentItem.getDescription().getIconUri();
                Bundle extras = currentItem.getDescription().getExtras();
                playingMediaDuration = extras != null ? extras.getLong(Keys.EXTRA_LENGTH) : 0L;
                if (!currentBitmapUriPair.second.equals(albumArtUri + "")) {
                    retryCount = 0;
                    loadAlbumArtAndPushNotification(albumArtUri);
                }
            }

            currentMediaIdOrVideoId = currentItem.getDescription().getMediaId();
            mMediaMetadataBuilder = new MediaMetadataCompat.Builder()
                    .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentItem.getDescription().getMediaId())
                    .putText(MediaMetadataCompat.METADATA_KEY_TITLE, currentItem.getDescription().getTitle())
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, currentItem.getDescription().getSubtitle())
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, currentItem.getDescription().getDescription())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtUri == null ? null : albumArtUri.toString())
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mediaIdLists.indexOf(currentItem.getDescription().getMediaId()))
                    .putLong(PlayerService.METADATA_KEY_FAVOURITE, Repository.getInstance(this).isAddedTo(currentItem.getDescription().getMediaId(), Keys.PLAYLISTS.FAVOURITES))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playingMediaDuration);
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("albumart_enabled", true))
                mMediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentBitmapUriPair.first);
            mSession.setMetadata(mMediaMetadataBuilder.build());
        } catch (Exception e) {
            LogHelper.e(TAG, "Error on setMediaMetadata: ", e);
            mMediaMetadataBuilder = new MediaMetadataCompat.Builder()
                    .putText(MediaMetadataCompat.METADATA_KEY_TITLE, "Media Playback Error")
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, "Corrupted media file")
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, "error")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, null)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mediaIdLists.indexOf(currentItem.getDescription().getMediaId()))
                    .putLong(PlayerService.METADATA_KEY_FAVOURITE, Repository.getInstance(this).isAddedTo(currentItem.getDescription().getMediaId(), Keys.PLAYLISTS.FAVOURITES))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0);
            mSession.setMetadata(mMediaMetadataBuilder.build());
        }

    }

    void pushNotification(long state) {
        LogHelper.d(TAG, "pushNotification: ");
        if (currentBitmapUriPair.first == null)
            return;
        MediaMetadataCompat metadata = mSession.getController().getMetadata();
        if (metadata == null)
            return;

        Bitmap bitmap = currentBitmapUriPair.first;// mSession.getController().getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        int minLength = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent notificationPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            notificationPendingIntent = PendingIntent.getActivity(this, 11, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            notificationPendingIntent = PendingIntent.getActivity(this, 11, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, Keys.Notification.CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notification_24)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mSession.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
                .setContentTitle(metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setSubText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setContentInfo(getString(R.string.app_name))
                .setLargeIcon(Bitmap.createBitmap(bitmap, (bitmap.getWidth() - minLength) / 2, (bitmap.getHeight() - minLength) / 2, minLength, minLength))
                .setContentIntent(notificationPendingIntent)
                .addAction(R.drawable.icon_skip_prev, getString(R.string.prev), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(state == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause : R.drawable.icon_play, getString(R.string.play_pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.icon_skip_next, getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .addAction(R.drawable.icon_close, getString(R.string.close), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setOngoing(!(state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_NONE || state == PlaybackStateCompat.STATE_PAUSED))
                .build();

        if (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_NONE) {
            notificationManager.notify(NOTIFICATION_ID, notification);
            removeAsForegroundService(false);
        } else if (state == PlaybackStateCompat.STATE_PAUSED) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            if (isForegroundService) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            } else {
                markAsForegroundService(notification);
            }
        }
    }


    ExoPlayer.EventListener ExoplayerEventListener = new ExoPlayer.EventListener() {
        @Override
        public void onPositionDiscontinuity(int reason) {
            switch (reason) {
                case ExoPlayer.DISCONTINUITY_REASON_PERIOD_TRANSITION:
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_PERIOD_TRANSITION :");
                    LogHelper.d(TAG, "onPositionDiscontinuity:  Window index: " + player.getCurrentWindowIndex());
                    if (queuePos != player.getCurrentWindowIndex()) {
                        queuePos = player.getCurrentWindowIndex();
                        setMediaMetadata(playingQueue.get(queuePos));
                        loadNextQueueItems();
                    }
                    setPlaybackState(player.getPlayWhenReady() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
                    pushNotification(player.getPlayWhenReady() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_SEEK:
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_SEEK ");
                    LogHelper.d(TAG, "onPositionDiscontinuity:  Window index : " + player.getCurrentWindowIndex() + " Play when ready : " + player.getPlayWhenReady());
                    if (player.getCurrentWindowIndex() == -1 || player.getCurrentWindowIndex() >= playingQueue.size())
                        return;
                    if (isSeek) {
                        isSeek = false;
                        return;
                    }
                    queuePos = player.getCurrentWindowIndex();
                    setMediaMetadata(playingQueue.get(queuePos));
                    pushNotification(player.getPlayWhenReady() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
                    loadNextQueueItems();
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_INTERNAL:
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_INTERNAL");
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_AD_INSERTION:
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_AD_INSERTION");
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_SEEK_ADJUSTMENT");
                    LogHelper.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_SEEK_ADJUSTMENT: quePos: " + player.getCurrentWindowIndex());
                    break;
                default:

            }
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            LogHelper.d(TAG, "onLoadingChanged: loading:" + isLoading);
            if (!isLoading)
                setPlaybackState(mSession.getController().getPlaybackState().getState());
            LogHelper.d(TAG, "onLoadingChanged: Duration: " + TimeUnit.MILLISECONDS.toSeconds(player.getBufferedPosition()) + "s");

        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            LogHelper.d(TAG, "IsPlayingChanged Playing: isPlaying - " + isPlaying);

            if (isPlaying) {
                LogHelper.d(TAG, "onIsPlayingChanged: Playing");
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                pushNotification(PlaybackStateCompat.STATE_PLAYING);
            } else {
                if (player != null) {
                    if (player.getPlaybackState() == ExoPlayer.STATE_BUFFERING) {
                        LogHelper.d(TAG, "onIsPlayingChanged: Buffering");
                        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                        pushNotification(PlaybackStateCompat.STATE_PLAYING);
                    } else {
                        LogHelper.d(TAG, "onIsPlayingChanged: Paused");
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        pushNotification(PlaybackStateCompat.STATE_PAUSED);
                    }
                }
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            error.printStackTrace();
            switch (error.type) {

                case ExoPlaybackException.TYPE_REMOTE:
                    LogHelper.d(TAG, "PlayerError: TYPE_REMOTE");
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    LogHelper.d(TAG, "PlayerError: TYPE_RENDERER");
                    break;
                case ExoPlaybackException.TYPE_SOURCE:
                    LogHelper.d(TAG, "PlayerError: TYPE_SOURCE");
                    if (playingMode == Keys.PLAYING_MODE.OFFLINE) {
                        Toast.makeText(PlayerService.this, "Unable to play! Skipping next", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(playNextOnMediaError, 2000);
                    } else {
                        if (!isInternetAvailable) {
                            playbackEndedStatus = PlaybackEndedStatus.INTERRUPTED;
                            Toast.makeText(PlayerService.this, "No Internet Access", Toast.LENGTH_SHORT).show();
                            mSession.getController().getTransportControls().pause();
                        } else {
                            if (mSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED || mSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED)
                                return;
                            Toast.makeText(PlayerService.this, "Hold on, please! URL is refreshing", Toast.LENGTH_SHORT).show();
                            uriCache.remove(playingQueue.get(queuePos).getDescription().getMediaId());
                            initializeAndPlay();
                        }
                    }
                    LogHelper.d(TAG, "PlayerError: curr indx = " + player.getCurrentWindowIndex() + " next indx = " + player.getNextWindowIndex());
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    LogHelper.d(TAG, "PlayerError: TYPE_UNEXPECTED");
                    break;
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, int reason) {
            LogHelper.d(TAG, "TimelineChanged: period_count: " + timeline.getPeriodCount());
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            LogHelper.d(TAG, "ExoPlayer playWhenReady: " + playWhenReady);
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    LogHelper.d(TAG, "ExoPlayer PlaybackState : STATE_BUFFERING");
                    setPlaybackState(playWhenReady ? PlaybackStateCompat.STATE_BUFFERING : PlaybackStateCompat.STATE_PAUSED);
                    break;
                case Player.STATE_ENDED:
                    LogHelper.d(TAG, "ExoPlayer PlaybackState: STATE_ENDED");
                    playbackEndedStatus = PlaybackEndedStatus.FINISHED;
                    mSession.getController().getTransportControls().stop();
                    break;
                case Player.STATE_IDLE:
                    LogHelper.d(TAG, "ExoPlayer PlaybackState: STATE_IDLE");
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    pushNotification(PlaybackStateCompat.STATE_STOPPED);
                    break;
                case Player.STATE_READY:
                    LogHelper.d(TAG, "ExoPlayer PlaybackState: STATE_READY");
                    break;
            }

        }

    };

    private void addToLastPlayed(MediaSessionCompat.QueueItem queueItem) {
        if (queueItem == null)
            return;
        try {
            String[] splits = queueItem.getDescription().getMediaId() != null ? queueItem.getDescription().getMediaId().split("[/|]") : new String[0];
            String playlist = Keys.PLAYLISTS.LAST_PLAYED;
            String mediaId = splits[splits.length - 1];
            String title = String.valueOf(queueItem.getDescription().getTitle());
            String artist = String.valueOf(queueItem.getDescription().getSubtitle());
            String album = String.valueOf(queueItem.getDescription().getDescription());
            String artwork = String.valueOf(queueItem.getDescription().getIconUri());
            Integer playListId = Repository.getInstance(PlayerService.this).getPlaylist(playlist).getId();
            MediaItem item = new MediaItem(mediaId, title, artist, album, playListId, artwork);

            LogHelper.d(TAG, "addToLastPlayed: " + item.getPlaylistId());
            Repository.getInstance(PlayerService.this).addLastPlayed(item);
        } catch (Exception e) {
            LogHelper.d(TAG, "Exception while adding to last played : \n" + ExceptionUtil.getStackStrace(e));
        }
    }

//    MediaSource getMediaSource(String mediaId) {
//        LogHelper.d(TAG, "MediaSource Media ID:" + mediaId);
//        String[] parts = mediaId.split("[/|]");
//        String id = parts[parts.length - 1];
//        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
//        DataSource.Factory factory = new DefaultDataSourceFactory(PlayerService.this, Util.getUserAgent(getApplicationContext(), "YM Player"));
//        return new ProgressiveMediaSource.Factory(factory).setTag(id).createMediaSource(contentUri);
//    }

    Pattern mediaIdPattern = Pattern.compile("[0-9]+");
    Pattern contentUriPattern = Pattern.compile("(content://|file://).*");
    int prevQuality = -1;

    void initDataSourceFactory() {
        extractorsFactory = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);
        factory = new ResolvingDataSource.Factory(new DefaultDataSourceFactory(this, Util.getUserAgent(PlayerService.this, "YM Player")), new ResolvingDataSource.Resolver() {
            @Override
            public DataSpec resolveDataSpec(DataSpec dataSpec) throws IOException {
                try {

                    LogHelper.d(TAG, "Resolving data spec for [===================================%s===================================]", dataSpec.uri);
                    boolean isMediaIdMatch = mediaIdPattern.matcher(dataSpec.uri.toString()).matches();
                    boolean isContentUriMatch = contentUriPattern.matcher(dataSpec.uri.toString()).matches();
                    String uriId = dataSpec.uri.toString();
                    LogHelper.d(TAG, "resolveDataSpec: %s", dataSpec);
                    LogHelper.d(TAG, "resolveDataSpec: pattern media id: matches: " + isMediaIdMatch);
                    LogHelper.d(TAG, "resolveDataSpec: pattern content uri: matches: " + isContentUriMatch);
                    if (isMediaIdMatch)
                        return dataSpec.withUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(uriId)));
                    else if (isContentUriMatch)
                        return dataSpec;
                    int quality = Integer.parseInt(Objects.requireNonNull(appPreferences.getString(Keys.PREFERENCE_KEYS.PLAYBACK_QUALITY, "2")));

                    LogHelper.d(TAG, "resolveDataSpec: found youtube song with videoId: " + uriId + "  & quality:" + quality);
                    LogHelper.d(TAG, "resolveDataSpec: available in cache : " + uriCache.contain(uriId));
                    Uri audioUri = null;
                    int index = mediaIdLists.indexOf(uriId);
                    if (uriCache.contain(uriId)) {
                        YoutubeSongUriDetail uriDetail = uriCache.get(uriId);
                        if (playingQueue.get(index).getDescription().getExtras() == null || !playingQueue.get(index).getDescription().getExtras().containsKey(Keys.EXTRA_LENGTH))
                            updatePlayingQueueItemLength(index, uriDetail.getLength());
                        Uri uri = uriDetail.getUri(quality);
                        LogHelper.d(TAG, "Found cached uri: " + uri);
                        return dataSpec.withUri(uri);
                    } else {
                        long start = SystemClock.currentThreadTimeMillis(), end;
                        LogHelper.d(TAG, "Fetching metadata using extractor");
                        VideoDetails videoDetails;
                        try {
                            videoDetails = extractor.extractV2(uriId);
                        } catch (Exception e) {
                            LogHelper.e(TAG, "Error while fetching metadata of " + uriId, e);
                            // retrying another time
                            videoDetails = extractor.extractV2(uriId);
                        }

                        end = SystemClock.currentThreadTimeMillis();
                        LogHelper.d(TAG, "resolveDataSpec: Exec time :" + (end - start) / 1000.0 + "s");
                        long songLength = ConverterUtil.convertStringLengthToLong(videoDetails.getVideoData().getLengthSeconds());
                        audioUri = updateUriInCacheAndGet(uriId, quality, videoDetails);
                        updatePlayingQueueItemLength(index, songLength);

                    }
                    prevQuality = quality;
                    return dataSpec.withUri(audioUri);
                } catch (ExtractionException e) {
                    LogHelper.e(TAG, "resolveDataSpec: error :", e);
                    return dataSpec;
                }


            }

        });
    }

    private Uri updateUriInCacheAndGet(String videoId, int quality, VideoDetails videoDetails) {
        Uri audioUri = null;
        if (videoDetails.getStreamingData() != null && !videoDetails.getStreamingData().getAdaptiveAudioFormats().isEmpty()) {
            List<StreamingData.AdaptiveAudioFormat> audioStreams = videoDetails.getStreamingData().getAdaptiveAudioFormats();
            long songLength = ConverterUtil.convertStringLengthToLong(videoDetails.getVideoData().getLengthSeconds());
            switch (audioStreams.size()) {
                case 1:
                    audioUri = Uri.parse(audioStreams.get(0).getUrl());
                    uriCache.put(videoId, new YoutubeSongUriDetail(videoId, audioStreams.get(0).getUrl(), audioStreams.get(0).getUrl(), audioStreams.get(0).getUrl(), songLength));
                    break;
                case 2:
                    audioUri = Uri.parse(audioStreams.get(quality / 2).getUrl());
                    uriCache.put(videoId, new YoutubeSongUriDetail(videoId, audioStreams.get(0).getUrl(), audioStreams.get(1).getUrl(), audioStreams.get(1).getUrl(), songLength));
                    break;
                case 3:
                    audioUri = Uri.parse(audioStreams.get(quality - 1).getUrl());
                    uriCache.put(videoId, new YoutubeSongUriDetail(videoId, audioStreams.get(0).getUrl(), audioStreams.get(1).getUrl(), audioStreams.get(2).getUrl(), songLength));
                    break;
                case 4:
                    audioUri = Uri.parse(audioStreams.get(quality).getUrl());
                    uriCache.put(videoId, new YoutubeSongUriDetail(videoId, audioStreams.get(1).getUrl(), audioStreams.get(2).getUrl(), audioStreams.get(3).getUrl(), songLength));
                    LogHelper.d(TAG, "resolveDataSpec: quality:" + quality + " selected bitrate:" + audioStreams.get(quality).getBitrate());
                    for (int i = 0; i < 4; i++)
                        LogHelper.d(TAG, "resolveDataSpec: bitrate : " + audioStreams.get(i).getBitrate());
                    break;

                case 0:
                    break;

                default:
                    Map<Integer, List<StreamingData.AdaptiveAudioFormat>> streamMap = new LinkedHashMap<>();
                    for (var stream : audioStreams) {
                        Integer itag = stream.getItag();
                        List<StreamingData.AdaptiveAudioFormat> adaptiveAudioFormats = streamMap.get(itag);
                        if (adaptiveAudioFormats == null) {
                            adaptiveAudioFormats = new ArrayList<>();
                            streamMap.put(itag, adaptiveAudioFormats);
                        }
                        adaptiveAudioFormats.add(stream);
                    }
                    int itag = getFromQuality(quality);
                    List<StreamingData.AdaptiveAudioFormat> audioFormats = streamMap.get(itag) != null ? streamMap.get(itag) : new ArrayList<>();
                    audioUri = Uri.parse(audioFormats.get(audioFormats.size() - 1).getUrl());
                    uriCache.put(videoId, new YoutubeSongUriDetail(videoId, audioStreams.get(1).getUrl(), audioStreams.get(2).getUrl(), audioStreams.get(3).getUrl(), songLength));
                    LogHelper.d(TAG, "resolveDataSpec: quality:" + quality + " selected bitrate:" + audioStreams.get(quality).getBitrate());
                    for (int i = 0; i < 4; i++)
                        LogHelper.d(TAG, "resolveDataSpec: bitrate : " + audioStreams.get(i).getBitrate());
                    break;
            }
            LogHelper.d(TAG, "resolveDataSpec: new uri:" + audioUri);

        } else LogHelper.d(TAG, "resolveDataSpec: No Audio Streams found");
        return audioUri;
    }

    private Integer getFromQuality(int quality) {
        switch (quality) {
            case 0:
                return 140;
            case 1:
                return 249;
            case 2:
                return 250;
            case 3:
                return 251;
        }
        return 140;
    }

    private void updatePlayingQueueItemLength(int index, long songLength) {
        Bundle ex = new Bundle();
        ex.putLong(Keys.EXTRA_LENGTH, songLength);
        MediaDescriptionCompat queueItem = playingQueue.get(index).getDescription();
        playingQueue.set(index, new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                .setMediaId(queueItem.getMediaId())
                .setTitle(queueItem.getTitle())
                .setSubtitle(queueItem.getSubtitle())
                .setDescription(queueItem.getDescription())
                .setExtras(ex)
                .setIconUri(queueItem.getIconUri())
                .build(),
                playingQueue.get(index).getQueueId()
        ));
        if (index == queuePos) setMediaMetadata(playingQueue.get(index));
    }

    @Override
    public void addHttpSourceToMediaSources(String videoId, int pos) {
        if (pos != -1)
            mediaSources.addMediaSource(pos, new ProgressiveMediaSource.Factory(factory, extractorsFactory).setTag(videoId).createMediaSource(Uri.parse(videoId)));
        else
            mediaSources.addMediaSource(new ProgressiveMediaSource.Factory(factory, extractorsFactory).setTag(videoId).createMediaSource(Uri.parse(videoId)));
    }

    @Override
    public void addURISourceToMediaSources(Uri uri, int pos) {
        mediaSources.addMediaSource(pos, new ProgressiveMediaSource.Factory(factory, extractorsFactory).setTag(uri.toString()).createMediaSource(uri));
    }

    @Override
    public void addToMediaSources(@Nullable String mediaId, int pos) {
        if (mediaId == null) return;
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        DataSource.Factory factory = new DefaultDataSourceFactory(this, Util.getUserAgent(getApplicationContext(), getString(R.string.app_name)));
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        if (pos != -1)
            mediaSources.addMediaSource(pos, new ProgressiveMediaSource.Factory(factory).setTag(id).createMediaSource(contentUri));
        else
            mediaSources.addMediaSource(new ProgressiveMediaSource.Factory(factory).setTag(id).createMediaSource(contentUri));
    }

    /**
     * @param player current instance of EXOPLAYER
     */
    @Override
    public void preparePlayer(SimpleExoPlayer player) {
        player.prepare(mediaSources, true, true);
        player.seekToDefaultPosition(queuePos);
    }

    @Override
    public void preparePlayer(SimpleExoPlayer player, long seekPosition) {
        player.prepare(mediaSources, true, true);
        player.seekTo(queuePos, seekPosition);
    }

    public void seekPlayer(int windowIndex, long seekPosition) {
        if (player == null) return;
        boolean isNoSeek = (!isQueueChanged && windowIndex == player.getCurrentWindowIndex() && player.getCurrentPosition() == C.TIME_UNSET);
        boolean isOnlySeek = (!isQueueChanged && windowIndex == player.getCurrentWindowIndex());
        LogHelper.d(TAG, "seekPlayer: seek : " + !isNoSeek + " only seek: " + isOnlySeek);
        if (isNoSeek) return;
        if (isOnlySeek)
            mSession.getController().getTransportControls().seekTo(seekPosition);
        else player.seekTo(windowIndex, seekPosition);
    }

    @Override
    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    @Override
    public void setPlayingQueueFromUri(Uri uri, Bundle extras) {
        String queueTitle = uri.toString();
        if (mSession.getController().getQueueTitle() != null && queueTitle.contentEquals(mSession.getController().getQueueTitle())) {
            LogHelper.d(TAG, "setPlayingQueue: .... Queue Already set");
            isQueueChanged = false;
            return;
        }
        mSession.setQueueTitle(queueTitle);

        playingQueue.clear();
        mediaIdLists.clear();
        if (player != null && player.isPlaying())
            player.stop();
        mediaSources.clear();
        playingQueue = Repository.getInstance(this).getCurrentPlayingQueue("URI|" + uri);
        prepareMediaSource();

    }


    @Override
    public SimpleExoPlayer getSimpleExoPlayer(SimpleExoPlayer player) {
        if (player == null) {
            player = new SimpleExoPlayer.Builder(this)
                    .setTrackSelector(new DefaultTrackSelector(this))
                    .build();
            player.addListener(ExoplayerEventListener);
            player.setRepeatMode(repeatMode);
            player.prepare(mediaSources);
            player.setPlayWhenReady(false);
            player.setShuffleModeEnabled(isShuffleEnabled());
            player.setHandleAudioBecomingNoisy(true);
            player.setAudioAttributes(new com.google.android.exoplayer2.audio.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC).build(), true);
            player.setWakeMode(C.WAKE_MODE_NETWORK);
            player.addAnalyticsListener(new AnalyticsListener() {
                @Override
                public void onAudioSessionIdChanged(EventTime eventTime, int audioSessionId) {
                    PlayerService.this.audioSessionId = audioSessionId;
                    Bundle extras = new Bundle();
                    extras.putInt(Keys.AUDIO_SESSION_ID, audioSessionId);
                    if (!resultReceivers.isEmpty()) {
                        for (ResultReceiver rr : resultReceivers)
                            rr.send(1001, extras);
                    }
                    loadEqualizer(audioSessionId);
                }
            });
        }
        return player;
    }

    /**
     * Stop player or play next song
     */
    Runnable playNextOnMediaError = new Runnable() {
        @Override
        public void run() {
            LogHelper.d(TAG, "playNextOnMediaError : Executed playNextOnMediaError");
            if (player.getNextWindowIndex() == -1)
                mSession.getController().getTransportControls().pause();
            else {
                queuePos = player.getNextWindowIndex();
                initializeAndPlay();
                LogHelper.d(TAG, "onPlayerError: Next");
            }
        }
    };

    private void initializeAndPlay() {
        LogHelper.d(TAG, "Initialising player");
        preparePlayer(player);
        player.setPlayWhenReady(true);
    }

    NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build();

    NetworkCallback networkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            isInternetAvailable = true;
        }

        @Override
        public void onLost(@NonNull Network network) {
            isInternetAvailable = false;
        }
    };

    enum PlaybackEndedStatus {
        INVALID, FINISHED, INTERRUPTED
    }

    void loadEqualizer(int audioSessionId) {
        EqualizerUtil.getInstance(this).updateAudioSessionId(audioSessionId);
    }


    private void markAsForegroundService(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        isForegroundService = true;
    }

    private void removeAsForegroundService(boolean removeNotification) {
        stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : STOP_FOREGROUND_DETACH);
        isForegroundService = false;
    }


    private boolean isShuffleEnabled() {
        return !PlayerHelperUtil.isDynamicQueue(queueTitleContext) && isShuffleModeEnabled;
    }

    private void loadNextQueueItems() {
        if(PlayerHelperUtil.isDynamicQueue(queueTitleContext) && queuePos >= playingQueue.size() - 5) {
            if(watchNextContinuationToken == null) {
                LogHelper.d(TAG, "loadWatchNextIfApplicable: No need to load as next token not available");
                return;
            }
            LogHelper.d(TAG, "loadWatchNextIfApplicable: loading watch next queue");
            appendWatchNextQueue(watchNextQueueUri, watchNextContinuationToken);
        }
    }




}



