package com.yash.ymplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.model.ResourceLoader;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.UriUtil;
import com.google.android.exoplayer2.util.Util;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.storage.MediaItem;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.media.AudioManager.STREAM_MUSIC;

public class PlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    public static final String STATE_PREF = "PlayerState";
    public static final String METADATA_KEY_LIKE = "like";
    public static final String MEDIA_ID = "mediaId";
    private static final String TAG = "debug";
    private static final String CHANNEL_ID = "channelOne";
    public static final String METADATA_KEY_FAVOURITE = "favourite";
    MediaSessionCompat mSession;
    PlaybackStateCompat.Builder mPlaybackStateBuilder;
    MediaMetadataCompat.Builder mMediaMetadataBuilder;
    String currentMediaId;
    SimpleExoPlayer player;
    List<MediaSessionCompat.QueueItem> playingQueue;
    List<String> mediaIdLists;
    int queuePos = -1;
    List<Song> songs;
    SharedPreferences preferences;
    long likeState = 0;
    Handler handler = new Handler();
    AudioManager audioManager;
    AudioFocusRequest audioFocusRequest;
    private long currentPlayerPosition;
    ConcatenatingMediaSource mediaSources;
    int repeatMode;
    boolean isShuffleModeEnabled;
    boolean isSeek;
    MediaSessionConnector mMediaSessionConnector;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service");
        IntentFilter noisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, noisyIntentFilter);

        //Variables
        songs = new ArrayList<>();
        playingQueue = new ArrayList<>();
        preferences = (SharedPreferences) getSharedPreferences(STATE_PREF, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        currentMediaId = preferences.getString(MEDIA_ID, null);
        mediaSources = new ConcatenatingMediaSource();
        mediaIdLists = new ArrayList<>();
        isSeek = false;
        repeatMode = preferences.getInt(Keys.REPEAT_MODE, 0);
        isShuffleModeEnabled = preferences.getBoolean(Keys.SHUFFLE_MODE, false);

        //MediaSession
        mSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
//        mMediaSessionConnector = new MediaSessionConnector(mSession);
//        mMediaSessionConnector.setPlayer(player);
        mSession.setCallback(mediaSessionCallbacks);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());
        setPlaybackState(PlaybackState.STATE_NONE);
        mSession.setRepeatMode(repeatMode);
        mSession.setShuffleMode(isShuffleModeEnabled ? PlaybackStateCompat.SHUFFLE_MODE_ALL : PlaybackStateCompat.SHUFFLE_MODE_NONE);
        mSession.setActive(true);


        //MediaPlayer
        player = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Handle Event");
        MediaButtonReceiver.handleIntent(mSession, intent);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("ROOT", null);
    }


    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

        result.detach();
        List<MediaBrowserCompat.MediaItem> mediaItems = null;
        if (parentId.equals("ROOT"))
            mediaItems = Repository.getInstance(this).getOfflineProvider().getAllSongs();
        result.sendResult(mediaItems);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
        Log.d(TAG, "onLoadChildren: PlayerSerice: Parent Id: " + parentId);
        result.detach();

        List<MediaBrowserCompat.MediaItem> mediaItems = null;
        if (parentId.equals("ALL_SONGS") || parentId.equals("ROOT")) {
            mediaItems = Repository.getInstance(this).getOfflineProvider().getAllSongs();
        } else if (parentId.contains("ARTISTS")) {
            if (parentId.equals("ARTISTS"))
                mediaItems = Repository.getInstance(this).getOfflineProvider().getAllArtists();
            else {
                mediaItems = Repository.getInstance(this).getOfflineProvider().getSongsOfArtist(parentId);
            }
        } else if (parentId.contains("ALBUMS")) {
            if (parentId.equals("ALBUMS"))
                mediaItems = Repository.getInstance(this).getOfflineProvider().getAllAlbums();
            else {
                mediaItems = Repository.getInstance(this).getOfflineProvider().getSongsOfAlbum(parentId);
            }
        } else if (parentId.contains("PLAYLISTS")) {
            if (parentId.equals("PLAYLISTS")) {
                mediaItems = Repository.getInstance(this).getAllPlaylists();
            } else {
                mediaItems = Repository.getInstance(this).getAllSongsOfPlaylist(parentId);
            }
        }
        Log.d(TAG, "onLoadChildren: PlayerService : MediaItem length - " + mediaItems.size());
        result.sendResult(mediaItems);
    }

    @Override
    public void onDestroy() {
        if (player != null)
            player.release();
        mSession.release();
        stopForeground(true);
        unregisterReceiver(noisyReceiver);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("mediaId", currentMediaId);
        editor.apply();
        Log.d(TAG, "onDestroy: Service Destroyed");
    }

    MediaSessionCompat.Callback mediaSessionCallbacks = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            Log.d(TAG, "onMediaButtonEvent: ");
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


        /**
         * Override to handle requests to play a specific mediaId that was
         * provided by your app's {@link MediaBrowserService}.
         *
         * @param mediaId id of the audio
         * @param extras any extra
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: extra: " + extras);
            currentMediaId = mediaId;
            if (!(mSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_NONE || mSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED))
                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
            setPlayingQueue(mediaId, extras);
            resolveQueuePosition(mediaId);
            if (!isAudioFocusGranted()) return;
            playRequest();
            Log.d(TAG, "onPlayFromMediaId: \n MediaID : " + currentMediaId + " \n Queue Position : " + queuePos +
                    " \n No Queue Items : " + playingQueue.size() + " \n Player Position: ");
        }

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay: currentMediaId:" + currentMediaId);
            if (isAudioFocusGranted()) {
                handlePlayRequest();
            }
        }


        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            Log.d(TAG, "onPause: ");
            if (player != null) {
                player.setPlayWhenReady(false);
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                pushNotification(PlaybackStateCompat.STATE_PAUSED);
            }


        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext: ");
            handler.removeCallbacks(playNextOnMediaError);
            if (!isAudioFocusGranted())
                return;
            Log.d(TAG, "onSkipToPrevious: " + (player.getPlaybackState() == ExoPlayer.STATE_ENDED) + " or idle :" + (player.getPlaybackState() == ExoPlayer.STATE_IDLE) + " state:" + player.getPlaybackState());

            if (player == null) {
                player = getSimpleExoPlayer(null);
                preparePlayer(player);
                if (player.getNextWindowIndex() == -1)
                    mSession.getController().getTransportControls().stop();
            } else if (player.getPlaybackState() == ExoPlayer.STATE_IDLE) {
                if (player.getNextWindowIndex() == -1)
                    mSession.getController().getTransportControls().stop();
                else {
                    queuePos = player.getNextWindowIndex();
                    preparePlayer(player);
                    player.setPlayWhenReady(true);
                }
            } else {
                player.next();
                player.setPlayWhenReady(true);
            }
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious: ");
            handler.removeCallbacks(playNextOnMediaError);
            if (!isAudioFocusGranted())
                return;

            if (player == null) {
                player = getSimpleExoPlayer(null);
                preparePlayer(player);
            } else if (player.getPlaybackState() == ExoPlayer.STATE_IDLE) {
                queuePos = player.getPreviousWindowIndex();
                preparePlayer(player);
                player.setPlayWhenReady(true);
            } else {
                Log.d(TAG, "onSkipToPrevious: " + player);
                if (player.getContentPosition() > 2000) {
                    onSeekTo(0);
                    player.setPlayWhenReady(true);
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                } else {
                    player.previous();
                    player.setPlayWhenReady(true);
                }
            }


        }

        /**
         * Override to handle requests to stop playback.
         */
        @SuppressWarnings("deprecation")
        @Override
        public void onStop() {
            Log.d(TAG, "onStop: ");
            player.stop();
            player.release();
            player = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(PlayerService.this);
            }
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            pushNotification(PlaybackStateCompat.STATE_STOPPED);
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         *
         * @param pos New position to move to, in milliseconds.
         */
        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo: " + pos);
            if (player != null) {
                isSeek = true;
                player.seekTo(pos);
                setPlaybackState(mSession.getController().getPlaybackState().getState());
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            Log.d(TAG, "onSetRepeatMode: ");
            player = getSimpleExoPlayer(player);
            PlayerService.this.repeatMode = repeatMode;
            player.setRepeatMode(repeatMode);
            mSession.setRepeatMode(repeatMode);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Keys.REPEAT_MODE, repeatMode);
            editor.apply();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            Log.d(TAG, "onSetShuffleMode: ");
            if (shuffleMode == 0) {
                isShuffleModeEnabled = false;
            } else if (shuffleMode > 0) {
                isShuffleModeEnabled = true;
            }
            player = getSimpleExoPlayer(player);
            player.setShuffleModeEnabled(isShuffleModeEnabled);
            mSession.setShuffleMode(shuffleMode);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Keys.SHUFFLE_MODE, isShuffleModeEnabled);
            editor.apply();

        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(TAG, "onCustomAction: ");
            switch (action) {
                case "like":
                    likeState = extras.getLong("like_enabled", 0);
                    Log.d(TAG, "onCustomAction: likeState: " + likeState);
                    setMediaMetadata(playingQueue.get(queuePos));
                    break;
                case Keys.Action.ADD_TO_PLAYLIST:
                    if (extras != null && extras.containsKey(Keys.PLAYLIST_NAME) && extras.containsKey(Keys.MEDIA_ID) && extras.containsKey(Keys.TITLE) && extras.containsKey(Keys.ARTIST) && extras.containsKey(Keys.ALBUM)) {
                        String playlist = extras.getString(Keys.PLAYLIST_NAME);
                        String mediaId = extras.getString(Keys.MEDIA_ID);
                        String title = extras.getString(Keys.TITLE);
                        String artist = extras.getString(Keys.ARTIST);
                        String album = extras.getString(Keys.ALBUM);
                        MediaItem item = new MediaItem(mediaId, title, artist, album, playlist);
                        if (Repository.getInstance(PlayerService.this).addToPlaylist(item) == -1)
                            Toast.makeText(PlayerService.this, "Already Added to " + playlist, Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(PlayerService.this, "Added to " + playlist, Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(PlayerService.this, "Please Select Playlist name", Toast.LENGTH_SHORT).show();
                    break;
                case Keys.Action.QUEUE_NEXT:
                    if (extras != null && extras.containsKey(Keys.MEDIA_ID)) {
                        String mediaId = extras.getString(Keys.MEDIA_ID);
                        Log.d(TAG, "onCustomAction: QUEUE_NEXT:MediaId:"+mediaId);
                        int pos = getPositionInQueue(mediaId);
                        Log.d(TAG, "onCustomAction: QUEUE_NEXT: " + pos);
                        if (pos == -1) {
                            List<MediaSessionCompat.QueueItem> item = Repository.getInstance(PlayerService.this).getOfflineProvider().getSongById(mediaId, (int) (queuePos + 1));
                            addToMediaSources(mediaId, (queuePos + 1));
                            playingQueue.addAll((queuePos + 1), item);
                            mediaIdLists.add((queuePos + 1), item.get(0).getDescription().getMediaId());
                        } else if (pos != queuePos) {
                            MediaSessionCompat.QueueItem item = playingQueue.get(pos);
                            if (pos > queuePos) {
                                playingQueue.remove(pos);
                                mediaIdLists.remove(pos);
                                playingQueue.add((queuePos + 1), item);
                                mediaIdLists.add((queuePos + 1), item.getDescription().getMediaId());
                                mediaSources.moveMediaSource(pos, queuePos + 1);
                            } else {
                                mediaSources.moveMediaSource(pos, queuePos);
                                playingQueue.add((queuePos + 1), item);
                                mediaIdLists.add((queuePos + 1), mediaIdLists.get(pos));
                                playingQueue.remove(pos);
                                mediaIdLists.remove(pos);
                                queuePos--;
                            }

                        }
                        mSession.setQueue(playingQueue);
                        setMediaMetadata(playingQueue.get(queuePos));
                    }
                    break;
                case Keys.Action.PLAY_FROM_QUEUE:
                    if (extras != null && extras.containsKey(Keys.QUEUE_POS)) {
                        int pos = extras.getInt(Keys.QUEUE_POS);
                        Log.d(TAG, "onCustomAction: Position :" + pos);
                        queuePos = pos;
                        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                        handlePlayRequest();
                    }
                    break;

                case Keys.Action.REMOVE_FROM_QUEUE:
                    if (extras != null && extras.containsKey(Keys.QUEUE_POS)) {
                        int pos = extras.getInt(Keys.QUEUE_POS);
                        mediaSources.removeMediaSource(pos);
                        playingQueue.remove(pos);
                        mediaIdLists.remove(pos);
                        if (playingQueue.size() != 0) {
                            if (pos == queuePos) {
                                queuePos = queuePos % playingQueue.size();
                                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                                handlePlayRequest();
                            } else if (pos < queuePos) queuePos--;
                            setMediaMetadata(playingQueue.get(queuePos));
                        } else {
                            mSession.setMetadata(null);
                            setPlaybackState(PlaybackStateCompat.STATE_NONE);
                            stopForeground(true);
                            player.release();
                            player = null;
                        }
                    }
                    break;

                case Keys.Action.SWAP_QUEUE_ITEM:
                    if (extras != null && extras.containsKey(Keys.FROM_POSITION) && extras.containsKey(Keys.TO_POSITION)) {
                        String mediaId = mediaIdLists.get(queuePos);
                        Log.d(TAG, "onCustomAction: SWAP_QUEUE_ITEM mediaid = "+mediaId+" pos:"+queuePos);
                        int fromPosition = extras.getInt(Keys.FROM_POSITION);
                        int toPosition = extras.getInt(Keys.TO_POSITION);
                        Log.d(TAG, "onCustomAction: from :"+fromPosition + " to :"+toPosition);
                        MediaSessionCompat.QueueItem item = playingQueue.remove(fromPosition);
                        playingQueue.add(toPosition,item);
                        String id = mediaIdLists.remove(fromPosition);
                        mediaIdLists.add(toPosition,id);
                        mediaSources.moveMediaSource(fromPosition, toPosition);
                        queuePos = mediaIdLists.indexOf(mediaId);
                        Log.d(TAG, "onCustomAction: new pos = "+queuePos);
                        mSession.setQueue(playingQueue);
                        setMediaMetadata(playingQueue.get(queuePos));
                    }
                    break;

                case Keys.Action.TOGGLE_FAVOURITE:
                    String[] parts = playingQueue.get(queuePos).getDescription().getMediaId().split("[/|]");
                    String mediaId = parts[parts.length - 1];
                    if (mSession.getController().getMetadata().getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0) {
                        Repository.getInstance(PlayerService.this).addToPlaylist(new MediaItem(mediaId, playingQueue.get(queuePos).getDescription().getTitle().toString(), playingQueue.get(queuePos).getDescription().getSubtitle().toString(), playingQueue.get(queuePos).getDescription().getDescription().toString(), Keys.PLAYLISTS.FAVOURITES));
                    } else {
                        Repository.getInstance(PlayerService.this).removeFromPlaylist(mediaId, Keys.PLAYLISTS.FAVOURITES);
                    }
                    setMediaMetadata(playingQueue.get(queuePos));
                    break;
                default:
            }


        }

    };

    /*
      Custom Methods
     */


    private int getPositionInQueue(@NonNull String mediaId) {
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        return mediaIdLists.indexOf(id);
    }

    /**
     * Extract the position of mediaId from Queue
     *
     * @param mediaId the id of current media
     */
    private void resolveQueuePosition(String mediaId) {
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        int pos = mediaIdLists.indexOf(id);
        if (pos != -1)
            queuePos = pos;
        else Log.d(TAG, "mediaId is not available in Queue");
    }

    /**
     * Obtain Playing Queue and Set to mediaSession
     * The Queue includes - MediaSources, Playing QueueItem
     *
     * @param mediaId the id of current media
     */
    void setPlayingQueue(String mediaId, Bundle extras) {
        mediaIdLists.clear();
        mediaSources.clear();
        if (!extras.containsKey(Keys.PLAY_SINGLE)) {
            playingQueue = Repository.getInstance(PlayerService.this).getCurrentPlayingQueue(mediaId);
            for (int i = 0; i < playingQueue.size(); i++) {
                String id = playingQueue.get(i).getDescription().getMediaId();
                mediaIdLists.add(id);
                addToMediaSources(id, i);
            }
            mSession.setQueue(playingQueue);
            mSession.setQueueTitle(mediaId.split("[|]")[0]);
            Log.d(TAG, "setPlayingQueue: All");
        } else if (extras.getBoolean(Keys.PLAY_SINGLE)) {
            playingQueue = Repository.getInstance(PlayerService.this).getOfflineProvider().getSongById(mediaId, 0);
            for (int i = 0; i < playingQueue.size(); i++) {
                String id = playingQueue.get(i).getDescription().getMediaId();
                mediaIdLists.add(id);
                addToMediaSources(id, i);
            }
            mSession.setQueue(playingQueue);
            mSession.setQueueTitle(mediaId.split("[|]")[0]);
            Log.d(TAG, "setPlayingQueue: Single");
        }

    }

    void handlePlayRequest() {
        if (currentMediaId == null) {
            if (preferences.getString("mediaId", null) != null) {
                mSession.getController().getTransportControls().playFromMediaId(preferences.getString("mediaId", null), null);
            }
        } else {
            playRequest();
        }
    }

    /**
     * Play Media based on PlaybackStates
     */
    void playRequest() {
        player = getSimpleExoPlayer(player);
        switch (mSession.getController().getPlaybackState().getState()) {
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_BUFFERING:
                preparePlayer(player);
                Log.d(TAG, "playRequest: window index" + player.getCurrentWindowIndex());
                player.setPlayWhenReady(true);
                break;

            case PlaybackStateCompat.STATE_PAUSED:
                player.setPlayWhenReady(true);
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                pushNotification(PlaybackStateCompat.STATE_PLAYING);
                break;
            default:
        }
    }


    /**
     * Build and set playback state to @link MediaSessionCompat
     *
     * @param state current playbackState
     */
    void setPlaybackState(int state) {
        mPlaybackStateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_URI | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, player != null ? player.getCurrentPosition() : 0, player != null ? player.getPlaybackParameters().speed : 1.0f);
        mSession.setPlaybackState(mPlaybackStateBuilder.build());
    }


    Bitmap currentBitmap;

    /**
     * Build and set mediaMetaData to MediaSessionCompat
     *
     * @param currentItem current mediaItem playing
     */
    void setMediaMetadata(MediaSessionCompat.QueueItem currentItem) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            Uri albumArtUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(currentItem.getDescription().getMediaId()));
            retriever.setDataSource(this, albumArtUri);
            byte[] data = retriever.getEmbeddedPicture();
            if (data != null)
                currentBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            else
                currentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.album_art_placeholder);
            mMediaMetadataBuilder = new MediaMetadataCompat.Builder()
                    .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,currentItem.getDescription().getMediaId())
                    .putText(MediaMetadataCompat.METADATA_KEY_TITLE, currentItem.getDescription().getTitle())
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, currentItem.getDescription().getSubtitle())
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, currentItem.getDescription().getDescription())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtUri.toString())
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mediaIdLists.indexOf(currentItem.getDescription().getMediaId()))
                    .putLong(PlayerService.METADATA_KEY_FAVOURITE, Repository.getInstance(this).isAddedTo(currentItem.getDescription().getMediaId(), Keys.PLAYLISTS.FAVOURITES))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("albumart_enabled", true))
                mMediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentBitmap);
            mSession.setMetadata(mMediaMetadataBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
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
        if (currentBitmap == null)
            return;
        Bitmap bitmap = currentBitmap;// mSession.getController().getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        int minLength = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 11, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notif)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mSession.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
                .setContentTitle(mSession.getController().getMetadata().getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(mSession.getController().getMetadata().getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setSubText(mSession.getController().getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setContentInfo("YM Player")
                .setLargeIcon(Bitmap.createBitmap(bitmap, (bitmap.getWidth() - minLength) / 2, (bitmap.getHeight() - minLength) / 2, minLength, minLength))
                .setContentIntent(notificationPendingIntent)
                .addAction(R.drawable.icon_skip_prev, PlayerService.this.getString(R.string.prev), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(state == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause : R.drawable.icon_play, PlayerService.this.getString(R.string.play_pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.icon_skip_next, PlayerService.this.getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .build();
        startForeground(10, notification);
        if (state != PlaybackStateCompat.STATE_PLAYING)
            stopForeground(false);
    }


    BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mSession.getController().getTransportControls().pause();
            }
            if (AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                mSession.getController().getTransportControls().play();
            }
        }
    };

    ExoPlayer.EventListener ExoplayerEventListener = new ExoPlayer.EventListener() {
        @Override
        public void onPositionDiscontinuity(int reason) {
            switch (reason) {
                case ExoPlayer.DISCONTINUITY_REASON_PERIOD_TRANSITION:
                    Log.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_PERIOD_TRANSITION : Window index: " + player.getCurrentWindowIndex());
                    if (queuePos != player.getCurrentWindowIndex()) {
                        queuePos = player.getCurrentWindowIndex();
                        setMediaMetadata(playingQueue.get(queuePos));
                    }
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    pushNotification(PlaybackStateCompat.STATE_PLAYING);
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_SEEK:
                    Log.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_SEEK : Window index : " + player.getCurrentWindowIndex() + " Queue Position : " + queuePos);
                    if (player.getCurrentWindowIndex() == -1) return;
                    if (isSeek) {
                        isSeek = false;
                        return;
                    }
                    queuePos = player.getCurrentWindowIndex();
                    player.setPlayWhenReady(true);
                    setMediaMetadata(playingQueue.get(queuePos));
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    pushNotification(PlaybackStateCompat.STATE_PLAYING);
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_INTERNAL:
                    Log.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_INTERNAL");
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_AD_INSERTION:
                    Log.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_AD_INSERTION");
                    break;
                case ExoPlayer.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                    Log.d(TAG, "onPositionDiscontinuity: DISCONTINUITY_REASON_SEEK_ADJUSTMENT");
                    break;
                default:

            }
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.d(TAG, "onLoadingChanged: loading:" + isLoading);
            Log.d(TAG, "onLoadingChanged: Duration: " + TimeUnit.MILLISECONDS.toSeconds(player.getBufferedPosition()) + "s");

        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG, "onIsPlayingChanged: Playing: " + isPlaying);

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            switch (error.type) {

                case ExoPlaybackException.TYPE_OUT_OF_MEMORY:
                    Log.d(TAG, "onPlayerError: TYPE_OUT_OF_MEMORY");
                    break;
                case ExoPlaybackException.TYPE_REMOTE:
                    Log.d(TAG, "onPlayerError: TYPE_REMOTE");
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    Log.d(TAG, "onPlayerError: TYPE_RENDERER");
                    break;
                case ExoPlaybackException.TYPE_SOURCE:
                    Log.d(TAG, "onPlayerError: TYPE_SOURCE");
                    Toast.makeText(PlayerService.this, "Unable to play! Skipping next", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onPlayerError: curr indx = " + player.getCurrentWindowIndex() + " next indx = " + player.getNextWindowIndex());
                    handler.postDelayed(playNextOnMediaError, 2000);

//                    mSession.getController().getTransportControls().skipToNext();
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    Log.d(TAG, "onPlayerError: TYPE_UNEXPECTED");
                    break;
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    Log.d(TAG, "onPlayerStateChanged: STATE_BUFFERING");
                    break;
                case Player.STATE_ENDED:
                    Log.d(TAG, "onPlayerStateChanged: STATE_ENDED");
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    mSession.getController().getTransportControls().stop();
                    break;
                case Player.STATE_IDLE:
                    Log.d(TAG, "onPlayerStateChanged: STATE_IDLE");
                    break;
                case Player.STATE_READY:
                    Log.d(TAG, "onPlayerStateChanged: STATE_READY");
                    break;
            }

        }

    };

    MediaSource getMediaSource(String mediaId) {
        Log.d(TAG, "MediaSource Media ID:" + mediaId);
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        DataSource.Factory factory = new DefaultDataSourceFactory(PlayerService.this, Util.getUserAgent(getApplicationContext(), "YM Player"));
        return new ProgressiveMediaSource.Factory(factory).setTag(id).createMediaSource(contentUri);
    }

    void addToMediaSources(@Nullable String mediaId, int pos) {
        if (mediaId == null) return;
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        DataSource.Factory factory = new DefaultDataSourceFactory(this, Util.getUserAgent(getApplicationContext(), "YM Player"));
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        mediaSources.addMediaSource(pos, new ProgressiveMediaSource.Factory(factory).setTag(id).createMediaSource(contentUri));
    }

    /**
     * @param player current instance of EXOPLAYER
     */
    void preparePlayer(SimpleExoPlayer player) {
        player.prepare(mediaSources, true, true);
        player.seekToDefaultPosition(queuePos);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: ");
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("mediaId", currentMediaId);
        editor.apply();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange: ");
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Permanent loss of audio focus
            // Pause playback immediately
            mSession.getController().getTransportControls().stop();
            // Wait 30 seconds before stopping playback
            handler.postDelayed(delayDeregisterAudioFocus,
                    TimeUnit.SECONDS.toMillis(30));
        } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
            mSession.getController().getTransportControls().pause();
            // Pause playback
        } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            player.setVolume(0.4f);
            // Lower the volume, keep playing
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Your app has been granted audio focus again
            // Raise volume to normal, restart playback if necessary
            handler.removeCallbacks(delayDeregisterAudioFocus);
            player.setVolume(1.0f);
            //handlePlayRequest();
        }
    }

    @SuppressWarnings("deprecation")
    Runnable delayDeregisterAudioFocus = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(PlayerService.this);
            }
        }
    };

    @SuppressWarnings("deprecation")
    boolean isAudioFocusGranted() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(PlayerService.this).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(PlayerService.this, STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        Log.d(TAG, "isAudioFocusGranted: " + (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED));
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    SimpleExoPlayer getSimpleExoPlayer(SimpleExoPlayer player) {
        if (player == null) {
            player = new SimpleExoPlayer.Builder(this).build();
            player.addListener(ExoplayerEventListener);
            player.setRepeatMode(repeatMode);
            player.setShuffleModeEnabled(isShuffleModeEnabled);
            player.prepare(mediaSources);
        }
        return player;
    }

    Runnable playNextOnMediaError = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: Executed playNextOnMediaError");
            if (player.getNextWindowIndex() == -1)
                mSession.getController().getTransportControls().stop();
            else {
                queuePos = player.getNextWindowIndex();
                preparePlayer(player);
                player.setPlayWhenReady(true);
                Log.d(TAG, "onPlayerError: Next");
            }
        }
    };
}

