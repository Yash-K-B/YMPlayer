package com.yash.ymplayer;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.equaliser.DialogEqualizerFragment;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.EqualizerUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.QueueListAdapter;
import com.yash.ymplayer.util.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public abstract class BasePlayerActivity extends BaseActivity implements ActivityActionProvider {

    public static final String STATE_PREF = "PlayerState";
    private static final String TAG = "BasePlayerActivity";
    private BasePlayerActivityBinding basePlayerActivityBinding;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    ActionBarDrawerToggle drawerToggle;
    Handler handler = new Handler(Looper.getMainLooper());
    SharedPreferences preferences;
    SharedPreferences defaultSharedPreferences;
    String currentFragment;  //current visible fragment
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduledFuture;
    long currentProgress;
    long currentBufferedPosition;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    String currentAlbumArtUri = null;
    boolean isPanelTopVisible = false;
    BottomSheetBehavior bottomSheetBehavior;
    BottomSheetBehavior playerView;
    List<Song> songs = new ArrayList<>();
    QueueListAdapter adapter;
    int previousPlayingPosition = -1;
    ItemTouchHelper itemTouchHelper;
    String currentMediaId;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    int navigationItemId = -1;
    boolean isQueueItemArranging;
    String queueTitle;
    DialogEqualizerFragment dialogEqualizerFragment;
    Pattern offlineAudioPattern;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LogHelper.d(TAG, "onCreate: BasePLayer");
        super.onCreate(savedInstanceState);
        basePlayerActivityBinding = BasePlayerActivityBinding.inflate(getLayoutInflater());
        setContentView(basePlayerActivityBinding.getRoot());
        startService(new Intent(this, PlayerService.class));
        setCustomToolbar(null, null);
//        basePlayerActivityBinding.songArt.setClipToOutline(true);
        bottomSheetBehavior = BottomSheetBehavior.from(basePlayerActivityBinding.playlists);
        preferences = getSharedPreferences(STATE_PREF, MODE_PRIVATE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        offlineAudioPattern = Pattern.compile("[0-9]+");

        playerView = BottomSheetBehavior.from(basePlayerActivityBinding.player);
        basePlayerActivityBinding.player.setOnClickListener(v -> {});
        playerView.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState != BottomSheetBehavior.STATE_HIDDEN && newState != BottomSheetBehavior.STATE_EXPANDED)
                    basePlayerActivityBinding.songTitle.setSelected(true);
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mediaController != null)
                        mediaController.getTransportControls().sendCustomAction(Keys.Action.CLOSE_PLAYBACK, null);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset > 0.5f) {
                    basePlayerActivityBinding.trackTitle.setSelected(true);
                    basePlayerActivityBinding.songTitle.setSelected(false);
                } else if (slideOffset >= 0f) {
                    basePlayerActivityBinding.trackTitle.setSelected(false);
                    basePlayerActivityBinding.songTitle.setSelected(true);
                } else {
                    basePlayerActivityBinding.trackTitle.setSelected(false);
                }

                basePlayerActivityBinding.playerTop.setAlpha(slideOffset >= 0.5f ? 0f : (1.0f - slideOffset * 2));
                basePlayerActivityBinding.playerTopBack.setAlpha(slideOffset <= 0.5f ? 0f : ((slideOffset - 0.5f) * 2));
                basePlayerActivityBinding.playerTop.setVisibility(0.95f <= slideOffset ? View.INVISIBLE : View.VISIBLE);
            }
        });
        playerView.setState(BottomSheetBehavior.STATE_HIDDEN);
        basePlayerActivityBinding.playerTop.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (playerView.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                playerView.setState(BottomSheetBehavior.STATE_EXPANDED);
        });


        adapter = new QueueListAdapter(this, new QueueListAdapter.QueueItemOnClickListener() {
            @Override
            public void onClick(Song song) {
                mediaController.getTransportControls().skipToQueueItem(songs.indexOf(song));
            }

            @Override
            public void onDelete(Song song) {
                int pos = songs.indexOf(song);
                songs.remove(pos);
                Bundle extra = new Bundle();
                extra.putString(Keys.MEDIA_ID, song.getId());
                mediaController.getTransportControls().sendCustomAction(Keys.Action.REMOVE_FROM_QUEUE, extra);
                adapter.notifyItemDeleted(pos);
            }

            @Override
            public void startDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        }, songs);
        basePlayerActivityBinding.playlistContainer.setAdapter(adapter);
        basePlayerActivityBinding.playlistContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        basePlayerActivityBinding.playlistContainer.setLayoutManager(new LinearLayoutManager(this));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(basePlayerActivityBinding.playlistContainer);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();
        onCreate(savedInstanceState, mediaBrowser, basePlayerActivityBinding);
    }

    abstract protected void onCreate(@Nullable Bundle savedInstanceState, MediaBrowserCompat mediaBrowser, BasePlayerActivityBinding playerActivityBinding);

    abstract protected void onConnected(MediaControllerCompat mediaController);

    @Override
    protected void onResume() {
        super.onResume();
        LogHelper.d(TAG, "onResume: ");
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            if (mediaController != null && mediaController.getMetadata() != null) {

                if (mediaController.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART) != null) {
                    Drawable resource = new BitmapDrawable(getResources(), mediaController.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
                    setSongArt(resource);
                    return;
                }

                executor.submit(() -> {
                    String songArt = mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                    boolean isUriSufficient = true;
                    if (songArt != null && songArt.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                        retriever.setDataSource(BasePlayerActivity.this, Uri.parse(songArt));
                        isUriSufficient = false;
                    }
                    songArt = String.format("https://i.ytimg.com/vi/%s/hqdefault.jpg", mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
                    Glide.with(BasePlayerActivity.this).load(isUriSufficient ? songArt : retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            setSongArt(resource);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            setSongArt();
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });


                });
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
            mediaBrowser = null;
        }
        if (mediaController != null) {
            mediaController.unregisterCallback(mediaControllerCallback);
            mediaController = null;
        }
    }


    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected: MainActivity");
            try {
                mediaController = new MediaControllerCompat(BasePlayerActivity.this, mediaBrowser.getSessionToken());
                mediaController.registerCallback(mediaControllerCallback);
                BasePlayerActivity.this.onConnected(mediaController);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            initialise();

        }
    };
    MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            queueTitle = title.toString();
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            switch (shuffleMode) {
                case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                    basePlayerActivityBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_off);
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                    basePlayerActivityBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_on);
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            switch (repeatMode) {
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_one);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_all);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_off);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            int color = getAttributeColor(BasePlayerActivity.this, R.attr.listTitleTextColor);
            //LogHelper.d(TAG, "onQueueChanged: queue size:" + queue.size() + " song size:" + songs.size() + " QueueUpdated: " + !(queueTitle != null && queueTitle.equals(Keys.QUEUE_TITLE.CUSTOM)));
            if (queueTitle != null && queueTitle.equals(Keys.QUEUE_TITLE.CUSTOM))
                return;
            songs.clear();
            for (int i = 0; i < queue.size(); i++) {
                //LogHelper.d(TAG, "QueueIem : " + queue.get(i).getDescription().getTitle());
                Song song = new Song(
                        queue.get(i).getDescription().getMediaId(),
                        queue.get(i).getDescription().getTitle() + "",
                        queue.get(i).getDescription().getSubtitle() + "",
                        color,
                        i);
                songs.add(song);
            }

            //previousPlayingPosition = -1;
            adapter.notifyQueueChange(previousPlayingPosition);
            LogHelper.d(TAG, "onQueueChanged: Adapter Notified");

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) return;
            LogHelper.d(TAG, "onMetadataChanged: song:" + metadata.getDescription().getTitle());
            if ((currentAlbumArtUri == null) || !currentAlbumArtUri.equalsIgnoreCase(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))) {
                currentAlbumArtUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                LogHelper.d(TAG, "onMetadataChanged: AlbumArt Uri:" + currentAlbumArtUri);
                boolean isUriSufficient = true;
                if (currentAlbumArtUri != null && currentAlbumArtUri.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    retriever.setDataSource(BasePlayerActivity.this, Uri.parse(currentAlbumArtUri));
                    isUriSufficient = false;
                }

                Glide.with(BasePlayerActivity.this).load(isUriSufficient ? currentAlbumArtUri : retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        LogHelper.d(TAG, "onResourceReady: uri:" + currentAlbumArtUri);
                        setSongArt(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        LogHelper.d(TAG, "onLoadFailed: uri:" + currentAlbumArtUri);
                        setSongArt();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
                basePlayerActivityBinding.songTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                basePlayerActivityBinding.trackTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                basePlayerActivityBinding.songSubtitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                basePlayerActivityBinding.trackSubTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            }
            basePlayerActivityBinding.maxDuration.setText(formatMillis(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            basePlayerActivityBinding.musicProgress.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            basePlayerActivityBinding.favouriteBtn.setImageResource(metadata.getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);

            currentMediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            LogHelper.d(TAG, "onMetadataChanged: Duration:" + metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000 + "s");
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (playerView.getState() == BottomSheetBehavior.STATE_HIDDEN && state.getState() != PlaybackStateCompat.STATE_NONE) {
                handler.post(() -> playerView.setState(BottomSheetBehavior.STATE_COLLAPSED));
            }
            if (state == null) return;
            long activeQueueItemId = state.getActiveQueueItemId();
            notifyCurrentPlayingSong((int) activeQueueItemId);

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    basePlayerActivityBinding.playPause.setImageResource(R.drawable.icon_pause);
                    basePlayerActivityBinding.playPauseBtn.setImageResource(R.drawable.icon_pause_circle);
                    basePlayerActivityBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    basePlayerActivityBinding.playerLoading.setVisibility(View.INVISIBLE);
                    currentProgress = state.getPosition();
                    currentBufferedPosition = state.getBufferedPosition();
                    basePlayerActivityBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
                    scheduledFutureUpdate();
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_PLAYING MainActivity");
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_STOPPED MainActivity");
                case PlaybackStateCompat.STATE_PAUSED:
                    basePlayerActivityBinding.playPause.setImageResource(R.drawable.icon_play);
                    basePlayerActivityBinding.playPauseBtn.setImageResource(R.drawable.icon_play_circle);
                    basePlayerActivityBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    basePlayerActivityBinding.playerLoading.setVisibility(View.INVISIBLE);
                    stopScheduledFutureUpdate();
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_PAUSED MainActivity");

                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    basePlayerActivityBinding.playPause.setImageResource(R.drawable.icon_pause);
                    basePlayerActivityBinding.playPauseBtn.setImageResource(R.drawable.icon_pause);
                    basePlayerActivityBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER);
                    basePlayerActivityBinding.playerLoading.setVisibility(View.VISIBLE);
                    currentProgress = state.getPosition();
                    currentBufferedPosition = state.getBufferedPosition();
                    basePlayerActivityBinding.musicProgress.setProgress((int) currentProgress);
                    basePlayerActivityBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
                    stopScheduledFutureUpdate();
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_BUFFERING MainActivity");
                    break;
                case PlaybackStateCompat.STATE_NONE:
                    stopScheduledFutureUpdate();
                    runOnUiThread(() -> playerView.setState(BottomSheetBehavior.STATE_HIDDEN));
                    break;
            }

        }


    };


//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        // Check if result comes from the correct activity
//        super.onActivityResult(requestCode, resultCode, data);
//        LogHelper.d(TAG, "onActivityResult: ");
//        if (requestCode == REQUEST_CODE) {
//            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
//            switch (response.getType()) {
//                // Response was successful and contains auth token
//                case TOKEN:
//                    SharedPreferences.Editor editor;
//                    editor = getSharedPreferences(Keys.SHARED_PREFERENCES.SPOTIFY, MODE_PRIVATE).edit();
//                    editor.putString(Keys.PREFERENCE_KEYS.TOKEN, response.getAccessToken());
//                    LogHelper.d(TAG, "GOT AUTH TOKEN");
//                    editor.apply();
//                    SpotifySongs spotifySongs = (SpotifySongs) getSupportFragmentManager().findFragmentByTag(Keys.Fragments.SPOTIFY_SONGS);
//                    if (spotifySongs != null) {
//                        getSupportFragmentManager().beginTransaction().detach(spotifySongs).commit();
//                        getSupportFragmentManager().beginTransaction().attach(spotifySongs).commit();
//                        spotifySongs.refresh();
//                    } else LogHelper.d(TAG, "onActivityResult: Null fragment");
//                    break;
//
//                // Auth flow returned an error
//                case ERROR:
//                    LogHelper.d(TAG, "onActivityResult: ERROR");
//                    // Handle error response
//                    break;
//
//                // Most likely auth flow was cancelled
//                default:
//                    LogHelper.d(TAG, "onActivityResult: default");
//                    // Handle other cases
//            }
//        }
//    }

    boolean dblClick = false;

    @Override
    public void onBackPressed() {

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else if (playerView.getState() == BottomSheetBehavior.STATE_EXPANDED)
            playerView.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void setCustomToolbar(Toolbar toolbar, String title) {
        if (toolbar == null) {
            basePlayerActivityBinding.mainToolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(basePlayerActivityBinding.mainToolbar);
        } else {
            basePlayerActivityBinding.mainToolbar.setVisibility(View.GONE);
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (title != null)
            getSupportActionBar().setTitle(title);
    }

    @Override
    public void setUserName(String name) {

    }

    @Override
    public void sendActionToMediaSession(String action, Bundle extras) {
        if (mediaController == null) return;
        mediaController.getTransportControls().sendCustomAction(action, extras);
    }

    @Override
    public void interactWithMediaSession(Callback callback) {
        if (mediaController == null) return;
        callback.trigger(mediaController);
    }

    void scheduledFutureUpdate() {
        stopScheduledFutureUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduledFuture = mExecutorService.scheduleAtFixedRate(() -> handler.post(() -> {
                basePlayerActivityBinding.musicProgress.setProgress((int) currentProgress);
                currentProgress += 250;
            }), 0, TimeUnit.MILLISECONDS.toMillis(250), TimeUnit.MILLISECONDS);
        }
    }

    void stopScheduledFutureUpdate() {
        if (mScheduledFuture != null)
            mScheduledFuture.cancel(false);
    }

    String formatMillis(long milli) {
        milli = milli / 1000;
        String hr = String.valueOf(milli / (3600));
        milli %= 3600;
        String min = milli / (60) > 9 ? String.valueOf(milli / (60)) : "0" + milli / (60);
        milli %= 60;
        String sec = milli > 9 ? String.valueOf(milli) : "0" + milli;

        if (hr.equals("0"))
            return min + ":" + sec;
        return hr + ":" + min + ":" + sec;
    }

    //Click listeners
    View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mediaController.getPlaybackState().getState() != PlaybackStateCompat.STATE_NONE) {
                if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING || mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_BUFFERING)
                    mediaController.getTransportControls().pause();
                else mediaController.getTransportControls().play();
            }
        }
    };
    View.OnClickListener skipNextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().skipToNext();
        }
    };
    View.OnClickListener skipPrevListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().skipToPrevious();
        }
    };
    View.OnClickListener repeatModeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().setRepeatMode((mediaController.getRepeatMode() + 1) % 3);
        }
    };

    View.OnClickListener shuffleModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().setShuffleMode((mediaController.getShuffleMode() + 1) % 2);
        }
    };
    View.OnClickListener shareSongListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String[] parts = currentMediaId.split("[|/]");
            if (offlineAudioPattern.matcher(parts[parts.length - 1]).matches()) {
                long mediaId = Long.parseLong(parts[parts.length - 1]);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Song via"));
            } else {
                String baseUrl = "https://youtu.be/";
                Intent shareIntent = new Intent(Intent.ACTION_VIEW);
                shareIntent.setData(Uri.parse(baseUrl + currentMediaId));
                startActivity(shareIntent);
            }

        }
    };


    View.OnClickListener toggleFavouriteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().sendCustomAction(Keys.Action.TOGGLE_FAVOURITE, null);
        }
    };

    View.OnClickListener currentPlaylistListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    };
    View.OnClickListener closeBottomSheetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    };
    View.OnClickListener openEqualizerListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isSystemEQ = !defaultSharedPreferences.getBoolean(Keys.PREFERENCE_KEYS.BUILTIN_EQUALIZER, false);
            Intent openEQIntent = new Intent();
            if (isSystemEQ) {
                openEQIntent.setAction(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                openEQIntent.resolveActivity(getPackageManager());
                startActivity(openEQIntent);

            } else {
                dialogEqualizerFragment = DialogEqualizerFragment.newBuilder()
                        .setAccentColor(BaseActivity.getAttributeColor(BasePlayerActivity.this, R.attr.colorAccent))  //Color.parseColor("#4caf50")
                        .setAudioSessionId(0)
                        .build();
                dialogEqualizerFragment.show(getSupportFragmentManager(), "eq");
            }


        }
    };
    SeekBar.OnSeekBarChangeListener progressListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            basePlayerActivityBinding.currentPos.setText(formatMillis(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            stopScheduledFutureUpdate();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mediaController.getTransportControls().seekTo(seekBar.getProgress());
        }
    };


    //Initialise
    void initialise() {

        EqualizerUtil.getInstance(this);

        mediaController.sendCommand(Keys.COMMAND.AUDIO_SESSION_ID, null, resultReceiver);

        switch (mediaController.getShuffleMode()) {
            case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                basePlayerActivityBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_off);
                break;
            case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                basePlayerActivityBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_on);
                break;
        }

        switch (mediaController.getRepeatMode()) {
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_off);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_one);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                basePlayerActivityBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_all);
                break;
        }
        if (mediaController.getQueue() != null) {
            List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
            songs.clear();
            int color = getAttributeColor(BasePlayerActivity.this, R.attr.listTitleTextColor);
            for (int i = 0; i < queue.size(); i++) {

                LogHelper.d(TAG, "QueueIem : " + queue.get(i).getDescription().getTitle());
                Song song = new Song(
                        queue.get(i).getDescription().getMediaId(),
                        queue.get(i).getDescription().getTitle().toString(),
                        queue.get(i).getDescription().getSubtitle().toString(),
                        color,
                        i);
                songs.add(song);
            }
            previousPlayingPosition = -1;
        }
        if (mediaController.getMetadata() != null) {
            currentMediaId = mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            currentAlbumArtUri = mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            boolean isSufficient = true;
            if (currentAlbumArtUri.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                isSufficient = false;
                retriever.setDataSource(this, Uri.parse(currentAlbumArtUri));
            }
            Glide.with(BasePlayerActivity.this).load(isSufficient ? currentAlbumArtUri : retriever.getEmbeddedPicture()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    setSongArt();
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    setSongArt(resource);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
            basePlayerActivityBinding.songTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            basePlayerActivityBinding.trackTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            basePlayerActivityBinding.songSubtitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            basePlayerActivityBinding.trackSubTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            basePlayerActivityBinding.playPause.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause : R.drawable.icon_play);
            basePlayerActivityBinding.playPauseBtn.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause_circle : R.drawable.icon_play_circle);
            basePlayerActivityBinding.maxDuration.setText(formatMillis(mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            basePlayerActivityBinding.musicProgress.setMax((int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            playerView.setState(BottomSheetBehavior.STATE_COLLAPSED);
//            int pos = (int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
//            notifyCurrentPlayingSong(pos);
            basePlayerActivityBinding.favouriteBtn.setImageResource(mediaController.getMetadata().getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);
        }
        if (mediaController.getPlaybackState() != null) {
            long activeQueuePosition = mediaController.getPlaybackState().getActiveQueueItemId();
            notifyCurrentPlayingSong((int) activeQueuePosition);
            switch (mediaController.getPlaybackState().getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    currentProgress = mediaController.getPlaybackState().getPosition();
                    currentBufferedPosition = mediaController.getPlaybackState().getBufferedPosition();
                    basePlayerActivityBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
                    scheduledFutureUpdate();
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                case PlaybackStateCompat.STATE_PAUSED:
                    basePlayerActivityBinding.currentPos.setText(formatMillis(mediaController.getPlaybackState().getPosition()));
                    basePlayerActivityBinding.musicProgress.setProgress((int) mediaController.getPlaybackState().getPosition());
                    break;
            }

        }
        basePlayerActivityBinding.playPause.setOnClickListener(playPauseListener);
        basePlayerActivityBinding.playPauseBtn.setOnClickListener(playPauseListener);
        basePlayerActivityBinding.next.setOnClickListener(skipNextListener);
        basePlayerActivityBinding.skipNextBtn.setOnClickListener(skipNextListener);
        basePlayerActivityBinding.skipPrevBtn.setOnClickListener(skipPrevListener);
        basePlayerActivityBinding.currentPlaylistBtn.setOnClickListener(currentPlaylistListener);
        basePlayerActivityBinding.repeatBtn.setOnClickListener(repeatModeClickListener);
        basePlayerActivityBinding.shuffleBtn.setOnClickListener(shuffleModeListener);
        basePlayerActivityBinding.favouriteBtn.setOnClickListener(toggleFavouriteListener);
        basePlayerActivityBinding.musicProgress.setOnSeekBarChangeListener(progressListener);
        basePlayerActivityBinding.closeBottomSheet.setOnClickListener(closeBottomSheetListener);
        basePlayerActivityBinding.minimize.setOnClickListener(v -> playerView.setState(BottomSheetBehavior.STATE_COLLAPSED));
        basePlayerActivityBinding.shareThis.setOnClickListener(shareSongListener);
        basePlayerActivityBinding.equalizer.setOnClickListener(openEqualizerListener);
    }

    void notifyCurrentPlayingSong(int currentPosition) {

        LogHelper.d(TAG, "notifyCurrentPlayingSong: current: " + currentPosition + " prev:" + previousPlayingPosition);
        if (currentPosition == -1 || songs == null || songs.size() <= currentPosition) return;
        if ((previousPlayingPosition == currentPosition && songs.get(currentPosition).getColor() == Color.GREEN) || isQueueItemArranging)
            return;
        LogHelper.d(TAG, "notifyCurrentPlayingSong: Adapter notified");
        adapter.notifyActiveItem(currentPosition);
        if (previousPlayingPosition != -1) {
            adapter.invalidateItem(previousPlayingPosition);
        }
        previousPlayingPosition = currentPosition;
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        int fromPosition = -1, toPosition = -1;

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            LogHelper.d(TAG, "onMove: Source Pos: " + viewHolder.getAdapterPosition() + " Target Pos: " + target.getAdapterPosition());
            isQueueItemArranging = true;
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            if (this.fromPosition == -1) {
                this.fromPosition = fromPosition;
            }
            this.toPosition = toPosition;
            Collections.swap(songs, fromPosition, toPosition);

            if (previousPlayingPosition == fromPosition)
                previousPlayingPosition = toPosition;
            else if (previousPlayingPosition > fromPosition && previousPlayingPosition <= toPosition)
                previousPlayingPosition--;
            else if (previousPlayingPosition < fromPosition && previousPlayingPosition >= toPosition)
                previousPlayingPosition++;
            adapter.notifyItemMoved(fromPosition, toPosition, previousPlayingPosition);
            return true;
        }


        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (fromPosition != -1 && toPosition != -1 && fromPosition != toPosition) {
                Bundle extras = new Bundle();
                extras.putInt(Keys.FROM_POSITION, fromPosition);
                extras.putInt(Keys.TO_POSITION, toPosition);
                mediaController.getTransportControls().sendCustomAction(Keys.Action.SWAP_QUEUE_ITEM, extras);
            }
            fromPosition = toPosition = -1;
            isQueueItemArranging = false;
        }
    };

    Runnable notifyAdapterDataChange = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
        }
    };

    ResultReceiver resultReceiver = new ResultReceiver(handler) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            LogHelper.d(TAG, "onReceiveResult: audioSessionId" + resultData.getInt(Keys.AUDIO_SESSION_ID));
//            basePlayerActivityBinding.visualizer.setColor(Color.GREEN);
//            basePlayerActivityBinding.visualizer.setPlayer(resultData.getInt(Keys.AUDIO_SESSION_ID));
        }
    };

    private void setSongArt(Drawable resource) {
        basePlayerActivityBinding.art.setImageDrawable(resource);
        basePlayerActivityBinding.songArt.setImageDrawable(resource);
        basePlayerActivityBinding.songArt.setBlur(3);
        basePlayerActivityBinding.songArt2.setImageDrawable(resource);
        basePlayerActivityBinding.songArt2.getLayoutParams().height = (int) ConverterUtil.getPx(this, resource.getIntrinsicHeight());
        basePlayerActivityBinding.songArt2.requestLayout();
        basePlayerActivityBinding.playerBlurBackground.setImageDrawable(resource);
        basePlayerActivityBinding.playerBlurBackground.setBlur(4);
    }

    private void setSongArt() {
        basePlayerActivityBinding.art.setImageResource(R.drawable.album_art_placeholder);
        basePlayerActivityBinding.songArt.setImageResource(R.drawable.album_art_placeholder);
        basePlayerActivityBinding.songArt2.setImageResource(R.drawable.album_art_placeholder);
        basePlayerActivityBinding.playerBlurBackground.setImageResource(R.drawable.bg_album_art);
        basePlayerActivityBinding.playerBlurBackground.setBlur(5);
    }
}
