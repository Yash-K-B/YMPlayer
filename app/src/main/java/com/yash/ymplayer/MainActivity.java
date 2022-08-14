package com.yash.ymplayer;

import android.Manifest;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.navigation.NavigationView;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yash.logging.utils.ExceptionUtil;
import com.yash.ymplayer.constant.Constants;
import com.yash.ymplayer.databinding.ActivityMainBinding;
import com.yash.ymplayer.equaliser.DialogEqualizerFragment;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.ui.main.AboutFragment;
import com.yash.ymplayer.ui.main.LocalSongs;
import com.yash.ymplayer.ui.main.SettingsFragment;
import com.yash.ymplayer.ui.youtube.YoutubeLibrary;
import com.yash.ymplayer.util.CommonUtil;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.EqualizerUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.QueueListAdapter;
import com.yash.ymplayer.util.Song;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class MainActivity extends BaseActivity implements ActivityActionProvider {
    public static final String STATE_PREF = "PlayerState";
    private static final String TAG = "MainActivity";
    public static final String EXTRA_CURRENT_FRAGMENT = "fragment";
    public static final String EXTRA_IS_PANEL_ACTIVE = "isPanelActive";
    public ActivityMainBinding activityMainBinding;
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
    Pattern deviceUriPattern;
    PopupMenu downloadPopup;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        startService(new Intent(this, PlayerService.class));
        setCustomToolbar(null, null);
        bottomSheetBehavior = BottomSheetBehavior.from(activityMainBinding.playlists);
        preferences = getSharedPreferences(STATE_PREF, MODE_PRIVATE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        offlineAudioPattern = Pattern.compile("[0-9]+");
        deviceUriPattern = Pattern.compile(Constants.DEVICE_URI_PREFIX_REGEX);
        downloadPopup = CommonUtil.buildYoutubeDownloadPopup(this, activityMainBinding.downloadBtn, () -> currentMediaId);


        //Display Exception (If any)
        if (defaultSharedPreferences.getBoolean(Keys.PREFERENCE_KEYS.IS_EXCEPTION, false)) {
            ScrollView scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
            horizontalScrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));


            String exception = defaultSharedPreferences.getString(Keys.PREFERENCE_KEYS.EXCEPTION, "");
            TextView tv = new TextView(this);
            tv.setGravity(GravityCompat.START);
            int px = (int) ConverterUtil.getPx(this, 20);
            tv.setPadding(px, px, px, px);
            tv.setText(exception);

            horizontalScrollView.addView(tv);
            scrollView.addView(horizontalScrollView);


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Exception")
                    .setView(scrollView)
                    .setOnDismissListener(dialog -> {
                        SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                        editor.putBoolean(Keys.PREFERENCE_KEYS.IS_EXCEPTION, false);
                        editor.apply();
                    })
                    .setPositiveButton("SEND", (dialog, which) -> {
                        executor.execute(() -> {
                            try {
                                URL url = new URL("https://y-dashboard.herokuapp.com/v1/createException");
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setRequestProperty("Accept", "application/json");
                                connection.setRequestProperty("Content-Type", "application/json");
                                connection.setDoInput(true);
                                connection.setDoInput(true);
                                Map<String, String> payloadMap = new HashMap<>();
                                payloadMap.put("deviceId", Build.MANUFACTURER + " " + Build.MODEL + "(API:" + Build.VERSION.SDK_INT + ")");
                                payloadMap.put("exceptionMessage", exception.replace("'", "\\'"));
                                payloadMap.put("timestamp", new Date().toString());
                                JSONObject payload = new JSONObject(payloadMap);
                                LogHelper.d(TAG, "payload of exception: " + payload);
                                OutputStream outputStream = connection.getOutputStream();
                                outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                                outputStream.close();
                                connection.connect();
                                InputStream stream = connection.getInputStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                                String r;
                                StringBuilder res = new StringBuilder();
                                while ((r = reader.readLine()) != null) {
                                    res.append(r);
                                }
                                LogHelper.d(TAG, res.toString());
                            } catch (IOException e) {
                                LogHelper.d(TAG, ConverterUtil.toStringException(e));
                            }
                        });
                        dialog.dismiss();
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        currentFragment = (getIntent().getStringExtra(EXTRA_CURRENT_FRAGMENT) != null) ? (getIntent().getStringExtra(EXTRA_CURRENT_FRAGMENT)) : "localSongs";
        playerView = BottomSheetBehavior.from(activityMainBinding.player);
        activityMainBinding.player.setOnClickListener(v -> playerView.setState(BottomSheetBehavior.STATE_EXPANDED));
        playerView.setState(BottomSheetBehavior.STATE_HIDDEN);
        playerView.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                expandOrCompressMainLayout(newState);
                if (newState != BottomSheetBehavior.STATE_HIDDEN && newState != BottomSheetBehavior.STATE_EXPANDED)
                    activityMainBinding.songTitle.setSelected(true);
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mediaController != null)
                        mediaController.getTransportControls().sendCustomAction(Keys.Action.CLOSE_PLAYBACK, null);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset > 0.5f) {
                    activityMainBinding.trackTitle.setSelected(true);
                    activityMainBinding.songTitle.setSelected(false);
                } else if (slideOffset >= 0f) {
                    activityMainBinding.trackTitle.setSelected(false);
                    activityMainBinding.songTitle.setSelected(true);
                } else {
                    activityMainBinding.trackTitle.setSelected(false);
                }

                activityMainBinding.playerTop.setAlpha(slideOffset >= 0.5f ? 0f : (1.0f - slideOffset * 2));
                activityMainBinding.playerTopBack.setAlpha(slideOffset <= 0.5f ? 0f : ((slideOffset - 0.5f) * 2));
                activityMainBinding.playerTop.setVisibility(0.95f <= slideOffset ? View.INVISIBLE : View.VISIBLE);
            }
        });
        ((TextView) activityMainBinding.navView.getHeaderView(0).findViewById(R.id.nav_header_text)).setText(defaultSharedPreferences.getString("user_name", "User@YMPlayer"));
        activityMainBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navigationItemId = item.getItemId();
                activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        activityMainBinding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                drawerToggle.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerToggle.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                LogHelper.d(TAG, "onDrawerClosed: ");
                if (navigationItemId == -1) return;
                switch (navigationItemId) {
                    case R.id.youtubeLibSongs:
                        if (currentFragment.equals(Keys.Fragments.YOUTUBE_SONGS)) return;
                        currentFragment = Keys.Fragments.YOUTUBE_SONGS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new YoutubeLibrary(), Keys.Fragments.YOUTUBE_SONGS).commit();
                        return;
                    case R.id.localSongs:
                        if (currentFragment.equals(Keys.Fragments.LOCAL_SONGS)) return;
                        currentFragment = Keys.Fragments.LOCAL_SONGS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
                        return;
                    case R.id.downloads:
                        if (currentFragment.equals(Keys.Fragments.DOWNLOADS)) return;
                        currentFragment = Keys.Fragments.DOWNLOADS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new DownloadFragment(), Keys.Fragments.DOWNLOADS).commit();
                        return;
                    case R.id.settings:
                        if (currentFragment.equals(Keys.Fragments.SETTINGS)) return;
                        currentFragment = Keys.Fragments.SETTINGS;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new SettingsFragment(), Keys.Fragments.SETTINGS).commit();
                        return;
                    case R.id.share:
                        shareMyApp();
                        return;
                    case R.id.about:
                        if (currentFragment.equals(Keys.Fragments.ABOUT)) return;
                        currentFragment = Keys.Fragments.ABOUT;
                        getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new AboutFragment(), Keys.Fragments.ABOUT).commit();
                        return;
                    default:
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                drawerToggle.onDrawerStateChanged(newState);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                LogHelper.d(TAG, "onCreate: Fragment Transaction:");
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
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 100);
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
            NotificationChannel defaultChannel = new NotificationChannel(Keys.Notification.CHANNEL_ID, Keys.Notification.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            defaultChannel.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            try {
                manager.createNotificationChannel(defaultChannel);
            } catch (NullPointerException e) {
                LogHelper.d(TAG, Arrays.toString(e.getStackTrace()));
            }

        }
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
        activityMainBinding.playlistContainer.setAdapter(adapter);
        activityMainBinding.playlistContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        activityMainBinding.playlistContainer.setLayoutManager(new LinearLayoutManager(this));
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(activityMainBinding.playlistContainer);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();
    }

    private void playIfIntentHasData(Intent intent) {
        LogHelper.d(TAG, "playIfIntentHasData: " + intent);
        if (mediaController != null && intent.getData() != null) {

            Uri data = intent.getData();
            Bundle extras = new Bundle();
            mediaController.getTransportControls().playFromUri(data, extras);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        playIfIntentHasData(intent);
    }

    private void expandOrCompressMainLayout(int newState) {
        LogHelper.d(TAG, "expandOrCompressMainLayout: " + newState);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) activityMainBinding.contentViewer.getLayoutParams();
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            if (layoutParams.bottomMargin != 0)
                return;
            ValueAnimator valueAnimator = ValueAnimator.ofInt(0, (int) ConverterUtil.getPx(this, 58));
            valueAnimator.addUpdateListener(valueAnimator1 -> {
                layoutParams.bottomMargin = (int) valueAnimator1.getAnimatedValue();
                activityMainBinding.contentViewer.setLayoutParams(layoutParams);
            });
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.start();
        } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            if (layoutParams.bottomMargin == 0)
                return;
            ValueAnimator valueAnimator = ValueAnimator.ofInt((int) ConverterUtil.getPx(this, 58), 0);
            valueAnimator.addUpdateListener(valueAnimator1 -> {
                layoutParams.bottomMargin = (int) valueAnimator1.getAnimatedValue();
                activityMainBinding.contentViewer.setLayoutParams(layoutParams);
            });
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.start();
        }
    }

    @Override
    public void refresh() {
        startActivity(new Intent(this, MainActivity.class).putExtra(EXTRA_CURRENT_FRAGMENT, currentFragment));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

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
                    if (songArt != null && deviceUriPattern.matcher(songArt).matches()) {
                        retriever.setDataSource(MainActivity.this, Uri.parse(songArt));
                        isUriSufficient = false;
                    }
                    songArt = String.format("https://i.ytimg.com/vi/%s/hqdefault.jpg", mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
                    Glide.with(MainActivity.this).load(isUriSufficient ? songArt : retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
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
        mediaBrowser.disconnect();
        mediaController.unregisterCallback(mediaControllerCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogHelper.d(TAG, "onRequestPermissionsResult: ");
        if (requestCode == 100) {
            for (int i = 0; i < permissions.length; i++)
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    finish();
            getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs()).commitAllowingStateLoss();
        }
    }

    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected: MainActivity");
            try {
                mediaController = new MediaControllerCompat(MainActivity.this, mediaBrowser.getSessionToken());
                mediaController.registerCallback(mediaControllerCallback);
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
                    break;
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            int color = getAttributeColor(MainActivity.this, R.attr.listTitleTextColor);
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
                if (currentAlbumArtUri != null && deviceUriPattern.matcher(currentAlbumArtUri).matches()) {
                    retriever.setDataSource(MainActivity.this, Uri.parse(currentAlbumArtUri));
                    isUriSufficient = false;
                }

                Glide.with(MainActivity.this).load(isUriSufficient ? currentAlbumArtUri : retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
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
                activityMainBinding.songTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                activityMainBinding.trackTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                activityMainBinding.songSubtitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                activityMainBinding.trackSubTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            }
            activityMainBinding.maxDuration.setText(formatMillis(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            activityMainBinding.musicProgress.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            activityMainBinding.favouriteBtn.setImageResource(metadata.getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);

            currentMediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            activityMainBinding.downloadBtn.setVisibility(CommonUtil.isYoutubeSong(currentMediaId)? View.VISIBLE: View.GONE);
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
                    activityMainBinding.playPause.setImageResource(R.drawable.icon_pause);
                    activityMainBinding.playPauseBtn.setImageResource(R.drawable.icon_pause_circle);
                    activityMainBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    activityMainBinding.playerLoading.setVisibility(View.INVISIBLE);
                    currentProgress = state.getPosition();
                    currentBufferedPosition = state.getBufferedPosition();
                    activityMainBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
                    scheduledFutureUpdate();
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_PLAYING MainActivity");
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_STOPPED MainActivity");
                case PlaybackStateCompat.STATE_PAUSED:
                    activityMainBinding.playPause.setImageResource(R.drawable.icon_play);
                    activityMainBinding.playPauseBtn.setImageResource(R.drawable.icon_play_circle);
                    activityMainBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    activityMainBinding.playerLoading.setVisibility(View.INVISIBLE);
                    stopScheduledFutureUpdate();
                    LogHelper.d(TAG, "onPlaybackStateChanged: STATE_PAUSED MainActivity");

                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    activityMainBinding.playPause.setImageResource(R.drawable.icon_pause);
                    activityMainBinding.playPauseBtn.setImageResource(R.drawable.icon_pause);
                    activityMainBinding.playPauseBtn.setScaleType(ImageView.ScaleType.CENTER);
                    activityMainBinding.playerLoading.setVisibility(View.VISIBLE);
                    currentProgress = state.getPosition();
                    currentBufferedPosition = state.getBufferedPosition();
                    activityMainBinding.musicProgress.setProgress((int) currentProgress);
                    activityMainBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
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
            case R.id.test:
                encodeToMP3();
                return true;
            case R.id.exit:
                LogHelper.d(TAG, "onOptionsItemSelected: Exit");
                Intent intent = new Intent(this.getApplicationContext(), PlayerService.class);
                stopService(intent);
                finish();
                System.exit(0);
                return true;
            default:
                return false;
        }

    }

    boolean dblClick = false;

    @Override
    public void onBackPressed() {

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else if (playerView.getState() == BottomSheetBehavior.STATE_EXPANDED)
            playerView.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else if (!getSupportFragmentManager().getFragments().contains(getSupportFragmentManager().findFragmentByTag(Keys.Fragments.LOCAL_SONGS))) {
            getSupportFragmentManager().beginTransaction().replace(activityMainBinding.container.getId(), new LocalSongs(), Keys.Fragments.LOCAL_SONGS).commit();
            activityMainBinding.navView.setCheckedItem(R.id.localSongs);
            navigationItemId = Objects.requireNonNull(activityMainBinding.navView.getCheckedItem()).getItemId();
            currentFragment = Keys.Fragments.LOCAL_SONGS;

        } else {
            if (dblClick)
                super.onBackPressed();
            else {
                dblClick = true;
                Toast.makeText(MainActivity.this, "Press again to exit", Toast.LENGTH_SHORT).show();
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
        //activityMainBinding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    public void setUserName(String name) {
        TextView userName = (activityMainBinding.navView.getHeaderView(0)).findViewById(R.id.nav_header_text);
        userName.setText(name);
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
                activityMainBinding.musicProgress.setProgress((int) currentProgress);
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


    View.OnClickListener toggleFavouriteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaController.getTransportControls().sendCustomAction(Keys.Action.TOGGLE_FAVOURITE, null);
        }
    };

    View.OnClickListener downloadSongListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            downloadPopup.show();
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
                        .setAccentColor(BaseActivity.getAttributeColor(MainActivity.this, R.attr.colorAccent))  //Color.parseColor("#4caf50")
                        .setAudioSessionId(0)
                        .build();
                dialogEqualizerFragment.show(getSupportFragmentManager(), "eq");
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

        EqualizerUtil.getInstance(this);

        mediaController.sendCommand(Keys.COMMAND.AUDIO_SESSION_ID, null, resultReceiver);

        // Play music if some one requested
        playIfIntentHasData(getIntent());

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
            int color = getAttributeColor(MainActivity.this, R.attr.listTitleTextColor);
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
            if (deviceUriPattern.matcher(currentAlbumArtUri).matches()) {
                isSufficient = false;
                retriever.setDataSource(this, Uri.parse(currentAlbumArtUri));
            }
            Glide.with(MainActivity.this).load(isSufficient ? currentAlbumArtUri : retriever.getEmbeddedPicture()).listener(new RequestListener<Drawable>() {
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
            activityMainBinding.songTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            activityMainBinding.trackTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            activityMainBinding.songSubtitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            activityMainBinding.trackSubTitle.setText(mediaController.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            activityMainBinding.playPause.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause : R.drawable.icon_play);
            activityMainBinding.playPauseBtn.setImageResource(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ? R.drawable.icon_pause_circle : R.drawable.icon_play_circle);
            activityMainBinding.maxDuration.setText(formatMillis(mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            activityMainBinding.musicProgress.setMax((int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            playerView.setState(BottomSheetBehavior.STATE_COLLAPSED);
            activityMainBinding.favouriteBtn.setImageResource(mediaController.getMetadata().getLong(PlayerService.METADATA_KEY_FAVOURITE) == 0 ? R.drawable.icon_favourite_off : R.drawable.icon_favourite);
            activityMainBinding.downloadBtn.setVisibility(CommonUtil.isYoutubeSong(currentMediaId)? View.VISIBLE: View.GONE);
        }
        if (mediaController.getPlaybackState() != null) {
            long activeQueuePosition = mediaController.getPlaybackState().getActiveQueueItemId();
            notifyCurrentPlayingSong((int) activeQueuePosition);
            switch (mediaController.getPlaybackState().getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    currentProgress = mediaController.getPlaybackState().getPosition();
                    currentBufferedPosition = mediaController.getPlaybackState().getBufferedPosition();
                    activityMainBinding.musicProgress.setSecondaryProgress((int) currentBufferedPosition);
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
        activityMainBinding.downloadBtn.setOnClickListener(downloadSongListener);
        activityMainBinding.musicProgress.setOnSeekBarChangeListener(progressListener);
        activityMainBinding.closeBottomSheet.setOnClickListener(closeBottomSheetListener);
        activityMainBinding.minimize.setOnClickListener(v -> playerView.setState(BottomSheetBehavior.STATE_COLLAPSED));
        activityMainBinding.shareThis.setOnClickListener(CommonUtil.buildShareSong(this, () -> currentMediaId));
        activityMainBinding.equalizer.setOnClickListener(openEqualizerListener);
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
            tempFile = new File(tempFile.getPath() + "/YM Player.apk");
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
            //Open share dialog
            Uri fileUri = FileProvider.getUriForFile(this, "com.yash.ymplayer.provider", tempFile);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share app via"));

        } catch (IOException e) {
            LogHelper.e(TAG, "shareMyApp: Error "+ ExceptionUtil.getStackStrace(e));
        }
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
//            activityMainBinding.visualizer.setColor(Color.GREEN);
//            activityMainBinding.visualizer.setPlayer(resultData.getInt(Keys.AUDIO_SESSION_ID));
        }
    };


    public static final int CHUNK_SIZE = 8192;

    void encodeToMP3() {
        List<Integer> x = null;
        x.add(5);

        for (File f : getExternalMediaDirs()) {
            LogHelper.d(TAG, "encodeToMP3: " + f.getName() + " " + f.getAbsolutePath());
        }
        ;
        LogHelper.d(TAG, "encodeToMP3: " + Environment.getRootDirectory().getName() + " " + Environment.getRootDirectory().getAbsolutePath());
        LogHelper.d(TAG, "encodeToMP3: " + getDir("YMPlayer", MODE_PRIVATE).getName() + " " + getDir("YMPlayer", MODE_PRIVATE).getAbsolutePath());
        LogHelper.d(TAG, "encodeToMP3: " + Environment.getExternalStoragePublicDirectory("YMPlayer").getName() + " " + Environment.getExternalStoragePublicDirectory("YMPlayer").getAbsolutePath());

//        for(File f : Environment.getExternalStoragePublicDirectory("YMPlayer")) {
//            LogHelper.d(TAG, "encodeToMP3: " + f.getName() + " "+ f.getAbsolutePath());
//        }

//        Intent downloadIntent = new Intent(MainActivity.this,DownloadService.class);
//        downloadIntent.putExtra(Keys.VIDEO_ID,"jjdjgfhj");
//        MainActivity.this.startService(downloadIntent);
//        extractID3Tags("/storage/emulated/0/Test/soja.mp3");
//        try {
//            MediaExtractor extractor = new MediaExtractor();
//            extractor.setDataSource("/storage/emulated/0/Test/soja.mp3");
//            LogHelper.d(TAG, "encodeToMP3: " + extractor.getTrackCount());
//
//            for (int i = 0; i < extractor.getTrackCount(); i++) {
//                MediaFormat format = extractor.getTrackFormat(i);
//                extractor.selectTrack(i);
//                LogHelper.d(TAG, "encodeToMP3: " + format.toString());
//            }
//
//
//            File file = new File("/storage/emulated/0/Test/soja_zara.pcm");
//            FileOutputStream outfile = new FileOutputStream(file);
//            // WritableByteChannel channel = Channels.newChannel(outfile);
//            //format.setInteger(MediaFormat.);
//            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_MPEG, 48000, 2);
//            format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
//            MediaCodec decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
//            decoder.configure(format, null, null, 0);
//            decoder.setCallback(new MediaCodec.Callback() {
//                @Override
//                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//                    LogHelper.d(TAG, "onInputBufferAvailable: index:" + index);
//                    ByteBuffer buffer = codec.getInputBuffer(index);
//                    // LogHelper.d(TAG, "onInputBufferAvailable: Buffer:" + buffer);
//                    int size = extractor.readSampleData(buffer, 0);
//                    LogHelper.d(TAG, "onInputBufferAvailable: x:" + size + " time :" + extractor.getSampleTime());
//                    if (size > 0) {
//                        codec.queueInputBuffer(index, 0, size, extractor.getSampleTime(), 0);
//                        extractor.advance();
//                    } else {
//                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                        LogHelper.d(TAG, "onInputBufferAvailable: End Of Stream");
//                    }
//
//                }
//
//                @Override
//                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                    LogHelper.d(TAG, "------------------------------------------onOutputBufferAvailable: " + index);
//                    ByteBuffer buffer = codec.getOutputBuffer(index);
//                    byte[] bytes = new byte[info.size - info.offset];
//                    int position = buffer.position();
//                    buffer.get(bytes);
//                    buffer.position(position);
//                    try {
//
//                        codec.releaseOutputBuffer(index, info.presentationTimeUs);
//                        outfile.write(bytes, 0, info.size - info.offset);
//                        LogHelper.d(TAG, "onOutputBufferAvailable: content: " + Arrays.toString(bytes));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//
//                @Override
//                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
//                    LogHelper.d(TAG, "onError: ");
//                }
//
//                @Override
//                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
//                    LogHelper.d(TAG, "onOutputFormatChanged: ");
//                    LogHelper.d(TAG, "OutputFormat: " + format);
//                    executor.execute(encodeToMP3);
//                }
//            });
//            decoder.start();
//
//            LogHelper.d(TAG, "encodeToMP3:" + decoder.getOutputFormat().toString());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //
        //executor.execute(encodeToMP3);
    }


    Runnable encodeToMP3 = () -> {
//        MediaExtractor extractor = new MediaExtractor();
//        try {
//            extractor.setDataSource("/storage/emulated/0/Test/soja.mp3");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        extractor.selectTrack(0);
//        MediaFormat format = extractor.getTrackFormat(0);
        LogHelper.d(TAG, "encodeToMP3: starting mp3 encode");
        String title = "Kanha Soja Zara (2017)";
        String artist = "Madhushree";
        String album = "Baahubali 2 - The Conclusion";


        LameBuilder builder = new LameBuilder()
                .setOutBitrate(320)
                .setOutChannels(2)
                .setOutSampleRate(48000)
                .setQuality(1)
                .setMode(LameBuilder.Mode.STEREO)
                .setId3tagTitle(title)
                .setId3tagArtist(artist)
                .setId3tagAlbum(album)
                .setId3tagYear("2017")
                .setId3tagComment(" extra ");
        AndroidLame androidLame = new AndroidLame(builder);

        byte[] mp3buff = new byte[CHUNK_SIZE * 8];
        short[] left = new short[CHUNK_SIZE * 8];
        short[] right = new short[CHUNK_SIZE * 8];
        try {
            File inFile = new File("/storage/emulated/0/Test/soja_zara.pcm");
            LogHelper.d(TAG, "encodeToMP3: File length : " + inFile.length());
            FileInputStream inputStream = new FileInputStream(inFile);
            File outfile = new File("/storage/emulated/0/Test/soja_zara.mp3");
            FileOutputStream outputStream = new FileOutputStream(outfile);
            while (true) {
                int byteRead = read(inputStream, left, right, CHUNK_SIZE * 8);
                LogHelper.d(TAG, "encodeToMP3: byteRead: " + byteRead);
                if (byteRead > 0) {
                    int byteEncode = androidLame.encode(left, right, byteRead, mp3buff);
                    outputStream.write(mp3buff, 0, byteEncode);
                    LogHelper.d(TAG, "encodeToMP3: byteEncode:" + byteEncode);
                } else break;
            }
            int outputMp3buf = androidLame.flush(mp3buff);
            if (outputMp3buf > 0) {
                outputStream.write(mp3buff, 0, outputMp3buf);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private static short byteToShortLE(byte b1, byte b2) {
        return (short) (b1 & 0xFF | ((b2 & 0xFF) << 8));
    }

    public int read(InputStream stream, short[] left, short[] right, int numSamples) throws
            IOException {

        byte[] buf = new byte[numSamples * 4];
        int index = 0;
        int bytesRead = stream.read(buf, 0, numSamples * 4);

        for (int i = 0; i < bytesRead; i += 2) {
            short val = byteToShortLE(buf[i], buf[i + 1]);
            if (i % 4 == 0) {
                left[index] = val;
            } else {
                right[index] = val;
                index++;
            }
        }

        return index;
    }


//    void extractID3Tags(String path) {
//        try {
//            File inpFile = new File(path);
//            FileInputStream iStream = new FileInputStream(inpFile);
//            byte[] bytes = new byte[128];
//            iStream.skip(inpFile.length()-128);
//            iStream.read(bytes);
//            LogHelper.d(TAG, "extractID3Tags: " + Arrays.toString(bytes));
//            if (bytes[0] == 'T' && bytes[1] == 'A' && bytes[2] == 'G') {
//                LogHelper.d(TAG, "Tag exists: ");
//                StringBuilder builder = new StringBuilder();
//                for (int i = 0; i < 30; i++) {
//                    builder.append((char) bytes[2+i]);
//                }
//                LogHelper.d(TAG, "Title: "+ builder.toString());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void setSongArt(Drawable resource) {
        activityMainBinding.art.setImageDrawable(resource);
        activityMainBinding.songArt.setImageDrawable(resource);
        activityMainBinding.songArt.setBlur(3);
        activityMainBinding.songArt2.setImageDrawable(resource);
        activityMainBinding.playerBlurBackground.setImageDrawable(resource);
        activityMainBinding.playerBlurBackground.setBlur(4);
    }

    private void setSongArt() {
        activityMainBinding.art.setImageResource(R.drawable.album_art_placeholder);
        activityMainBinding.songArt.setImageResource(R.drawable.album_art_placeholder);
        activityMainBinding.songArt2.setImageResource(R.drawable.album_art_placeholder);
        activityMainBinding.playerBlurBackground.setImageResource(R.drawable.bg_album_art);
        activityMainBinding.playerBlurBackground.setBlur(5);
    }

}