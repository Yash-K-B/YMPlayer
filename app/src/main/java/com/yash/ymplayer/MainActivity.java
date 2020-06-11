package com.yash.ymplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.Callback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yash.ymplayer.databinding.ActivityMainBinding;
import com.yash.ymplayer.databinding.NavHeaderLayoutBinding;
import com.yash.ymplayer.ui.main.AboutFragment;
import com.yash.ymplayer.ui.main.LocalSongs;
import com.yash.ymplayer.ui.main.SettingsFragment;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.QueueListAdapter;
import com.yash.ymplayer.util.Song;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsListAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity implements ActivityActionProvider {
    public static final String STATE_PREF = "PlayerState";
    private static final String TAG = "debug";
    private static final String CHANNEL_ID = "channelOne";
    private static final CharSequence CHANNEL_NAME = "Default Channel";
    public static final String EXTRA_CURRENT_FRAGMENT = "fragment";
    public static final String EXTRA_IS_PANEL_ACTIVE = "isPanelActive";
    public ActivityMainBinding activityMainBinding;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    ActionBarDrawerToggle drawerToggle;
    Handler handler = new Handler();
    SharedPreferences preferences;
    SharedPreferences defaultSharedPreferences;
    String currentFragment;  //current visible fragment
    private ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduledFuture;
    long currentProgress;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    String currentAlbumArtUri = null;
    boolean isPanelTopVisible = false;
    BottomSheetBehavior bottomSheetBehavior;
    List<Song> songs = new ArrayList<>();
    QueueListAdapter adapter;
    int previousPlayingPosition = -1;
    ItemTouchHelper itemTouchHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        startService(new Intent(this, PlayerService.class));
        setCustomToolbar(null, null);
        activityMainBinding.songArt.setClipToOutline(true);
        bottomSheetBehavior = BottomSheetBehavior.from(activityMainBinding.playlists);
        preferences = getSharedPreferences(STATE_PREF, MODE_PRIVATE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentFragment = (getIntent().getStringExtra(EXTRA_CURRENT_FRAGMENT) != null) ? (getIntent().getStringExtra(EXTRA_CURRENT_FRAGMENT)) : "localSongs";
        activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        activityMainBinding.slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset > 0.9) {
                    if (isPanelTopVisible) {
                        activityMainBinding.playerTop.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_out));
                        activityMainBinding.playerTop.setVisibility(View.INVISIBLE);
                        isPanelTopVisible = false;
                    }
                    activityMainBinding.trackTitle.setSelected(true);
                    activityMainBinding.songTitle.setSelected(false);
                } else if (slideOffset >= 0) {
                    if (!isPanelTopVisible) {
                        activityMainBinding.playerTop.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
                        activityMainBinding.playerTop.setVisibility(View.VISIBLE);
                        isPanelTopVisible = true;
                    }

                    activityMainBinding.trackTitle.setSelected(false);
                } else {
                    activityMainBinding.playerTop.setVisibility(View.VISIBLE);
                    isPanelTopVisible = true;
                    activityMainBinding.trackTitle.setSelected(false);
                }
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState != SlidingUpPanelLayout.PanelState.HIDDEN && newState != SlidingUpPanelLayout.PanelState.EXPANDED)
                    activityMainBinding.songTitle.setSelected(true);
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED || newState == SlidingUpPanelLayout.PanelState.HIDDEN)
                    if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED)
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        ((TextView) activityMainBinding.navView.getHeaderView(0).findViewById(R.id.nav_header_text)).setText(defaultSharedPreferences.getString("user_name", "User@YMPlayer"));
        activityMainBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.localSongs:
                        currentFragment = Keys.Fragments.LOCAL_SONGS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
                        activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.settings:
                        currentFragment = Keys.Fragments.SETTINGS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new SettingsFragment(), Keys.Fragments.SETTINGS).commit();
                        activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.share:
                        shareMyApp();
                        activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.about:
                        currentFragment = Keys.Fragments.ABOUT;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new AboutFragment(), Keys.Fragments.ABOUT).commit();
                        activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START);
//                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                        builder.setTitle("About")
//                                .setMessage("Application version: " + BuildConfig.VERSION_NAME)
//                                .setPositiveButton("Cancel", (dialog, which) -> dialog.dismiss());
//                        AlertDialog alertDialog = builder.create();
//                        alertDialog.show();
                        return true;
                    default:
                        return false;
                }

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: Fragment Transaction:");
                switch (currentFragment) {
                    case Keys.Fragments.SETTINGS:
                        activityMainBinding.navView.setCheckedItem(R.id.settings);
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new SettingsFragment(), Keys.Fragments.SETTINGS).commit();
                        break;
                    case Keys.Fragments.ABOUT:
                        activityMainBinding.navView.setCheckedItem(R.id.about);
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new AboutFragment(), Keys.Fragments.ABOUT).commit();
                        break;
                    default:
                        activityMainBinding.navView.setCheckedItem(R.id.localSongs);
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
                        break;
                }

            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        } else {

            switch (currentFragment) {
                case Keys.Fragments.SETTINGS:
                    activityMainBinding.navView.setCheckedItem(R.id.settings);
                    getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new SettingsFragment(), Keys.Fragments.SETTINGS).commit();
                    break;
                case Keys.Fragments.ABOUT:
                    activityMainBinding.navView.setCheckedItem(R.id.about);
                    getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new AboutFragment(), Keys.Fragments.ABOUT).commit();
                    break;
                default:
                    activityMainBinding.navView.setCheckedItem(R.id.localSongs);
                    getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
                    break;
            }


        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel defaultChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            defaultChannel.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(defaultChannel);
        }
        adapter = new QueueListAdapter(this, new QueueListAdapter.QueueItemOnClickListener() {
            @Override
            public void onClick(Song song) {
                Bundle extra = new Bundle();
                extra.putInt(Keys.QUEUE_POS, songs.indexOf(song));
                mediaController.getTransportControls().sendCustomAction(Keys.Action.PLAY_FROM_QUEUE, extra);
            }

            @Override
            public void onDelete(Song song) {
                int pos = songs.indexOf(song);
                songs.remove(pos);
                Bundle extra = new Bundle();
                extra.putInt(Keys.QUEUE_POS, pos);
                mediaController.getTransportControls().sendCustomAction(Keys.Action.REMOVE_FROM_QUEUE, extra);
                adapter.notifyItemRemoved(pos);
            }

            @Override
            public void startDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        }, songs);
        activityMainBinding.playlistContainer.setAdapter(adapter);
        activityMainBinding.playlistContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        activityMainBinding.playlistContainer.setLayoutManager(new LinearLayoutManager(this));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(activityMainBinding.playlistContainer);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();

    }

    @Override
    public void refresh() {
        startActivity(new Intent(this, MainActivity.class).putExtra(EXTRA_CURRENT_FRAGMENT, currentFragment));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaBrowser.disconnect();
        mediaController.unregisterCallback(mediaControllerCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        if (requestCode == 100) {
            for (int i = 0; i < permissions.length; i++)
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs()).commitAllowingStateLoss();
                else finish();
        }
    }

    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected: MainActivity");
            try {
                mediaController = new MediaControllerCompat(MainActivity.this, mediaBrowser.getSessionToken());
                mediaController.registerCallback(mediaControllerCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            initialise();

        }
    };
    private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            switch (shuffleMode) {
                case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                    activityMainBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_off);
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                    activityMainBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_on);
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            switch (repeatMode) {
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_one);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_all);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_off);
                    break;
                default:
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            songs.clear();
            int color = getAttributeColor(R.attr.listTitleTextColor);
            for (int i = 0; i < queue.size(); i++) {

                Log.d(TAG, "QueueIem : " + queue.get(i).getDescription().getTitle());
                Song song = new Song(
                        queue.get(i).getDescription().getMediaId(),
                        queue.get(i).getDescription().getTitle().toString(),
                        queue.get(i).getDescription().getSubtitle().toString(),
                        color,
                        i);
                songs.add(song);
            }
            previousPlayingPosition = -1;
            adapter.notifyDataSetChanged();
            Log.d(TAG, "onQueueChanged: Adapter Notified");
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) return;
            Log.d(TAG, "onMetadataChanged: song:" + metadata.getDescription().getTitle());
            if ((currentAlbumArtUri == null) || !currentAlbumArtUri.equalsIgnoreCase(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))) {
                currentAlbumArtUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                retriever.setDataSource(MainActivity.this, Uri.parse(currentAlbumArtUri));
                Glide.with(MainActivity.this).load(retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.d(TAG, "onLoadFailed: ");
                        activityMainBinding.art.setImageResource(R.drawable.album_art_placeholder);
                        activityMainBinding.songArt.setImageResource(R.drawable.album_art_placeholder);
                        activityMainBinding.playerBlurBackground.setImageResource(R.drawable.bg_album_art);
                        activityMainBinding.playerBlurBackground.setBlur(5);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                }).into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Log.d(TAG, "onResourceReady: target");
                        activityMainBinding.art.setImageDrawable(resource);
                        activityMainBinding.songArt.setImageDrawable(resource);
                        activityMainBinding.playerBlurBackground.setImageDrawable(resource);
                        activityMainBinding.playerBlurBackground.setBlur(5);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
                activityMainBinding.songTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                activityMainBinding.trackTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                activityMainBinding.songSubtitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                activityMainBinding.trackSubTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            }
            activityMainBinding.maxDuration.setText(formatMillis(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            activityMainBinding.musicProgress.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            activityMainBinding.favouriteBtn.setImageResource(metadata.getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);

            int pos = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            notifyCurrentPlayingSong(pos);
            Log.d(TAG, "onMetadataChanged: Duration:" + metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (activityMainBinding.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                handler.postDelayed(() -> {
                    activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }, 200);
            }
            if (state == null) return;
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    activityMainBinding.playPause.setImageResource(R.drawable.icon_pause);
                    activityMainBinding.playPauseBtn.setImageResource(R.drawable.icon_pause_circle);
                    currentProgress = state.getPosition();
                    scheduledFutureUpdate();
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                case PlaybackStateCompat.STATE_STOPPED:
                    activityMainBinding.playPause.setImageResource(R.drawable.icon_play);
                    activityMainBinding.playPauseBtn.setImageResource(R.drawable.icon_play_circle);
                    stopScheduledFutureUpdate();
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    break;
                case PlaybackStateCompat.STATE_NONE:
                    stopScheduledFutureUpdate();
                    handler.postDelayed(() -> activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN), 300);
                    break;
            }

        }


    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activityMainBinding.drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.exit:
                Log.d(TAG, "onOptionsItemSelected: Exit");
                Intent intent = new Intent(this.getApplicationContext(), PlayerService.class);
                stopService(intent);
                finish();
                return true;
            default:
                return false;
        }

    }

    boolean dblClick = false;

    @Override
    public void onBackPressed() {

        if (activityMainBinding.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        else if (!getSupportFragmentManager().getFragments().contains(getSupportFragmentManager().findFragmentByTag(Keys.Fragments.LOCAL_SONGS))) {
            getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
            activityMainBinding.navView.setCheckedItem(R.id.localSongs);
        } else {
            if (dblClick)
                super.onBackPressed();
            else {
                dblClick = true;
                Toast.makeText(MainActivity.this, "Press Again to Exit", Toast.LENGTH_SHORT).show();
                handler.postDelayed(() -> dblClick = false, 2000);
            }
        }
    }

    @Override
    public void setCustomToolbar(Toolbar toolbar, String title) {
        if (toolbar == null) {
            activityMainBinding.mainToolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(activityMainBinding.mainToolbar);
        } else {
            activityMainBinding.mainToolbar.setVisibility(View.GONE);
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (title != null)
            getSupportActionBar().setTitle(title);
        drawerToggle = new ActionBarDrawerToggle(this, activityMainBinding.drawerLayout, R.string.open, R.string.close);
        activityMainBinding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    public void setUserName(String name) {
        TextView userName = (activityMainBinding.navView.getHeaderView(0)).findViewById(R.id.nav_header_text);
        userName.setText(name);
    }


    void scheduledFutureUpdate() {
        stopScheduledFutureUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduledFuture = mExecutorService.scheduleAtFixedRate(() -> handler.post(() -> {
                activityMainBinding.musicProgress.setProgress((int) currentProgress);

                if (currentProgress > activityMainBinding.musicProgress.getMax())
                    currentProgress = currentProgress - activityMainBinding.musicProgress.getMax();
                else currentProgress += 250;
            }), 0, TimeUnit.MILLISECONDS.toMillis(250), TimeUnit.MILLISECONDS);
        }
    }

    void stopScheduledFutureUpdate() {
        if (mScheduledFuture != null)
            mScheduledFuture.cancel(false);
    }

    String formatMillis(long milli) {
        milli = milli / 1000;
        String hr = milli / (3600) > 9 ? String.valueOf(milli / (3600)) : "0" + milli / (3600);
        milli %= 3600;
        String min = milli / (60) > 9 ? String.valueOf(milli / (60)) : "0" + milli / (60);
        milli %= 60;
        String sec = milli > 9 ? String.valueOf(milli) : "0" + milli;

        return min + ":" + sec;
    }

    //Click listeners
    View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mediaController.getPlaybackState().getState() != PlaybackStateCompat.STATE_NONE) {
                if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
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
    SeekBar.OnSeekBarChangeListener progressListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            activityMainBinding.currentPos.setText(formatMillis(progress));
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

        switch (mediaController.getShuffleMode()) {
            case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                activityMainBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_off);
                break;
            case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                activityMainBinding.shuffleBtn.setImageResource(R.drawable.exo_icon_shuffle_on);
                break;
        }

        switch (mediaController.getRepeatMode()) {
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_off);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_one);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                activityMainBinding.repeatBtn.setImageResource(R.drawable.exo_controls_repeat_all);
                break;
        }
        if (mediaController.getQueue() != null) {
            List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
            songs.clear();
            int color = getAttributeColor(R.attr.listTitleTextColor);
            for (int i = 0; i < queue.size(); i++) {

                Log.d(TAG, "QueueIem : " + queue.get(i).getDescription().getTitle());
                Song song = new Song(
                        queue.get(i).getDescription().getMediaId(),
                        queue.get(i).getDescription().getTitle().toString(),
                        queue.get(i).getDescription().getSubtitle().toString(),
                        color,
                        i);
                songs.add(song);
            }
            previousPlayingPosition = -1;
            adapter.notifyDataSetChanged();
        }

        if (mediaController.getMetadata() != null) {
            currentAlbumArtUri = mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            retriever.setDataSource(this, Uri.parse(currentAlbumArtUri));
            Glide.with(MainActivity.this).load(retriever.getEmbeddedPicture()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    activityMainBinding.art.setImageResource(R.drawable.album_art_placeholder);
                    activityMainBinding.songArt.setImageResource(R.drawable.album_art_placeholder);
                    activityMainBinding.playerBlurBackground.setImageResource(R.drawable.bg_album_art);
                    activityMainBinding.playerBlurBackground.setBlur(5);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    activityMainBinding.art.setImageDrawable(resource);
                    activityMainBinding.songArt.setImageDrawable(resource);
                    activityMainBinding.playerBlurBackground.setImageDrawable(resource);
                    activityMainBinding.playerBlurBackground.setBlur(4);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
            activityMainBinding.songTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            activityMainBinding.trackTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            activityMainBinding.songSubtitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            activityMainBinding.trackSubTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            activityMainBinding.playPause.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause : R.drawable.icon_play);
            activityMainBinding.playPauseBtn.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause_circle : R.drawable.icon_play_circle);
            activityMainBinding.maxDuration.setText(formatMillis(mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            activityMainBinding.musicProgress.setMax((int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            int pos = (int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            notifyCurrentPlayingSong(pos);
            activityMainBinding.favouriteBtn.setImageResource(mediaController.getMetadata().getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);
        }
        if (mediaController.getPlaybackState() != null) {
            switch (mediaController.getPlaybackState().getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    currentProgress = mediaController.getPlaybackState().getPosition();
                    scheduledFutureUpdate();
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                case PlaybackStateCompat.STATE_PAUSED:
                    activityMainBinding.currentPos.setText(formatMillis(mediaController.getPlaybackState().getPosition()));
                    activityMainBinding.musicProgress.setProgress((int) mediaController.getPlaybackState().getPosition());
                    break;
            }

        }
        activityMainBinding.playPause.setOnClickListener(playPauseListener);
        activityMainBinding.playPauseBtn.setOnClickListener(playPauseListener);
        activityMainBinding.next.setOnClickListener(skipNextListener);
        activityMainBinding.skipNextBtn.setOnClickListener(skipNextListener);
        activityMainBinding.skipPrevBtn.setOnClickListener(skipPrevListener);
        activityMainBinding.currentPlaylistBtn.setOnClickListener(currentPlaylistListener);
        activityMainBinding.repeatBtn.setOnClickListener(repeatModeClickListener);
        activityMainBinding.shuffleBtn.setOnClickListener(shuffleModeListener);
        activityMainBinding.favouriteBtn.setOnClickListener(toggleFavouriteListener);
        activityMainBinding.musicProgress.setOnSeekBarChangeListener(progressListener);
        activityMainBinding.closeBottomSheet.setOnClickListener(closeBottomSheetListener);
        activityMainBinding.minimize.setOnClickListener(v -> activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));

    }

    public int getAttributeColor(int resId) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(resId, value, true);
        return value.data;
    }

    private void shareMyApp() {
        ApplicationInfo app = getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;

        Intent intent = new Intent(Intent.ACTION_SEND);

        // MIME of .apk is "application/vnd.android.package-archive".
        // but Bluetooth does not accept this. Let's use "*/*" instead.
        intent.setType("*/*");

        // Append file and send Intent
        File originalApk = new File(filePath);

        try {
            //Make new directory in new location
            File tempFile = new File(getExternalCacheDir() + "/ExtractedApk");
            //If directory doesn't exists create new
            if (!tempFile.isDirectory())
                if (!tempFile.mkdirs())
                    return;
            //Get application's name and convert to lowercase
            tempFile = new File(tempFile.getPath() + "/" + getString(app.labelRes).replace(" ", "").toLowerCase() + ".apk");
            //If file doesn't exists create new
            if (!tempFile.exists()) {
                if (!tempFile.createNewFile()) {
                    return;
                }
            }
            //Copy file to new location
            InputStream in = new FileInputStream(originalApk);
            OutputStream out = new FileOutputStream(tempFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            System.out.println("File copied.");
            //Open share dialog
            Uri fileUri = FileProvider.getUriForFile(this, "com.yash.ymplayer.provider", tempFile);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share app via"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void notifyCurrentPlayingSong(int currentPosition) {

        Log.d(TAG, "notifyCurrentPlayingSong: current: " + currentPosition + " prev:" + previousPlayingPosition);
        songs.get(currentPosition).setColor(Color.GREEN);
        adapter.notifyItemChanged(currentPosition);
        if (previousPlayingPosition != -1 && currentPosition != previousPlayingPosition && previousPlayingPosition < songs.size()) {
            songs.get(previousPlayingPosition).setColor(getAttributeColor(R.attr.listTitleTextColor));
            adapter.notifyItemChanged(previousPlayingPosition);
        }
        previousPlayingPosition = currentPosition;
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            Log.d(TAG, "onMove: Source Pos: " + viewHolder.getAdapterPosition() + " Target Pos: " + target.getAdapterPosition());
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            Collections.swap(songs, fromPosition, toPosition);
            Bundle extras = new Bundle();
            extras.putInt(Keys.FROM_POSITION, fromPosition);
            extras.putInt(Keys.TO_POSITION, toPosition);
            mediaController.getTransportControls().sendCustomAction(Keys.Action.SWAP_QUEUE_ITEM, extras);
            if (previousPlayingPosition == fromPosition)
                previousPlayingPosition = toPosition;
            else if (previousPlayingPosition > fromPosition && previousPlayingPosition <= toPosition)
                previousPlayingPosition--;
            else if (previousPlayingPosition < fromPosition && previousPlayingPosition >= toPosition)
                previousPlayingPosition++;
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };


}