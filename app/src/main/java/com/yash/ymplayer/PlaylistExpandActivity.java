package com.yash.ymplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.databinding.PlaylistExpandActivityBinding;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.ui.youtube.YoutubeTracksAdapter;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;

public class PlaylistExpandActivity extends BasePlayerActivity {
    private static final String TAG = "PlaylistExpandActivity";
    PlaylistExpandActivityBinding activityBinding;
    String playlistId;
    String title;
    String headerArt;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, MediaBrowserCompat mediaBrowser, BasePlayerActivityBinding playerActivityBinding) {
        activityBinding = PlaylistExpandActivityBinding.inflate(getLayoutInflater());
        playerActivityBinding.container.addView(activityBinding.getRoot());

        Intent intent = getIntent();
        playlistId = intent.getStringExtra(Keys.EXTRA_PARENT_ID);
        title = intent.getStringExtra(Keys.EXTRA_TITLE);
        headerArt = intent.getStringExtra(Keys.EXTRA_ART_URL);
        setCustomToolbar(activityBinding.toolbar, title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        activityBinding.tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityBinding.listProgress.setVisibility(View.VISIBLE);
                activityBinding.dialogTryAgain.setVisibility(View.INVISIBLE);
                load(mediaController);
            }
        });
        postponeEnterTransition();
        Glide.with(this).load(headerArt).into(activityBinding.appBarImage);
    }

    @Override
    protected void onConnected(MediaControllerCompat mediaController) {
        this.mediaController = mediaController;
        load(mediaController);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finishAfterTransition();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refresh() {

    }

    void load(MediaControllerCompat mediaController) {
        if (mediaController == null)
            return;
        OnlineYoutubeRepository.getInstance(PlaylistExpandActivity.this).getPlaylistTracks(playlistId, title, new OnlineYoutubeRepository.TracksLoadedCallback() {
            @Override
            public void onLoaded(List<YoutubeSong> songs) {
                activityBinding.listProgress.setVisibility(View.GONE);
                YoutubeTracksAdapter adapter = new YoutubeTracksAdapter(PlaylistExpandActivity.this, songs, new TrackClickListener() {
                    @Override
                    public void onClick(YoutubeSong song) {
                        String id = playlistId + "|" + song.getVideoId();
                        LogHelper.d(TAG, "onClick: uri" + id);
                        mediaController.getTransportControls().playFromMediaId(id, null);
                    }

                    @Override
                    public void onPlaySingle(YoutubeSong song) {
                        Bundle extra = new Bundle();
                        extra.putBoolean(Keys.PLAY_SINGLE, true);
                        mediaController.getTransportControls().playFromMediaId(playlistId + "|" + song.getVideoId(), extra);
                    }

                    @Override
                    public void onQueueNext(YoutubeSong song) {
                        Bundle extras = new Bundle();
                        extras.putString(Keys.MEDIA_ID, playlistId + "|" + song.getVideoId());
                        extras.putInt(Keys.QUEUE_HINT, AudioProvider.QueueHint.YOUTUBE_SINGLE_SONG);
                        extras.putString(Keys.QUEUE_MODE, Keys.QueueMode.ONLINE.name());
                        mediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT, extras);
                    }

                    @Override
                    public void onQueueLast(YoutubeSong song) {
                        Bundle extras = new Bundle();
                        extras.putString(Keys.MEDIA_ID, playlistId + "|" + song.getVideoId());
                        extras.putInt(Keys.QUEUE_HINT, AudioProvider.QueueHint.YOUTUBE_SINGLE_SONG);
                        extras.putString(Keys.QUEUE_MODE, Keys.QueueMode.ONLINE.name());
                        mediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_LAST, extras);
                    }
                });
                activityBinding.list.setLayoutManager(new LinearLayoutManager(PlaylistExpandActivity.this));
                activityBinding.list.setAdapter(adapter);
            }

            @Override
            public <E extends Exception> void onError(E e) {
                LogHelper.d(TAG, "onError: ");
                activityBinding.listProgress.setVisibility(View.INVISIBLE);
                activityBinding.dialogTryAgain.setVisibility(View.VISIBLE);
            }
        });
    }
}
