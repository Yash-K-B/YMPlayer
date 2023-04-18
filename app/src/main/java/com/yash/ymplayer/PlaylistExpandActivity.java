package com.yash.ymplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.databinding.PlaylistExpandActivityBinding;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.YoutubeTracksAdapter;
import com.yash.ymplayer.ui.youtube.adapters.LoadStateFooterAdapter;
import com.yash.ymplayer.ui.youtube.livepage.YoutubePagedListAdapter;
import com.yash.ymplayer.ui.youtube.toptracks.TopTracks;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.TrackContextMenuClickListener;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import lombok.val;

public class PlaylistExpandActivity extends BasePlayerActivity {
    private static final String TAG = "PlaylistExpandActivity";
    PlaylistExpandActivityBinding activityBinding;
    String playlistId;
    String title;
    String headerArt;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;

    YoutubePagedListAdapter pagedListAdapter;

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

        activityBinding.list.setLayoutManager(new LinearLayoutManager(PlaylistExpandActivity.this));
        pagedListAdapter = new YoutubePagedListAdapter(PlaylistExpandActivity.this, new TrackContextMenuClickListener(PlaylistExpandActivity.this, mediaController, playlistId + "|"), mediaController);
        activityBinding.list.setAdapter(pagedListAdapter.withLoadStateFooter(new LoadStateFooterAdapter()));
        activityBinding.tryAgain.setOnClickListener(v -> {
            activityBinding.listProgress.setVisibility(View.VISIBLE);
            activityBinding.dialogTryAgain.setVisibility(View.INVISIBLE);
            load(mediaController);
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
        YoutubeLibraryViewModel viewModel = new ViewModelProvider(this).get(YoutubeLibraryViewModel.class);
        viewModel.getPlaylistTracks(playlistId).observe(this, youtubeSongs -> {
            LogHelper.d(TAG, "load: [%s] -> size: [%s]", playlistId, youtubeSongs);
            pagedListAdapter.submitData(getLifecycle(), youtubeSongs);
            activityBinding.listProgress.setVisibility(View.GONE);
        });
    }
}
