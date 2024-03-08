package com.yash.ymplayer;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.databinding.ListExpandActivityBinding;
import com.yash.ymplayer.ui.main.LocalViewModel;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsContextMenuClickListener;
import com.yash.ymplayer.util.SongsListAdapter;

import java.util.List;

public class ListExpandActivity extends BasePlayerActivity {
    private static final String TAG = "debug";
    ListExpandActivityBinding binding;
    MediaBrowserCompat mMediaBrowser;
    LocalViewModel viewModel;
    MediaControllerCompat mMediaController;
    String type = null;
    String parentId;
    String title;
    Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, MediaBrowserCompat mediaBrowser, BasePlayerActivityBinding playerActivityBinding) {
        binding = ListExpandActivityBinding.inflate(getLayoutInflater());
        playerActivityBinding.container.addView(binding.getRoot());
        viewModel = new ViewModelProvider(ListExpandActivity.this).get(LocalViewModel.class);
        mMediaBrowser = mediaBrowser;
        Intent intent = getIntent();
        parentId = intent.getStringExtra(Keys.EXTRA_PARENT_ID);
        type = intent.getStringExtra(Keys.EXTRA_TYPE);
        title = intent.getStringExtra(Keys.EXTRA_TITLE);
        setCustomToolbar(binding.toolbar, title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;
    }

    @Override
    protected void onConnected(MediaControllerCompat mediaController) {
        mMediaController = mediaController;
        if (type.equalsIgnoreCase("artist")) {
            artistTracks();
        } else if (type.equalsIgnoreCase("album")) {
            albumTracks();
        } else if (type.equalsIgnoreCase("playlist")) {
            playListTracks();
        }
        binding.shuffle.setOnClickListener(v -> {
            SongsListAdapter songsAdapter = (SongsListAdapter) binding.listRv.getAdapter();
            if(songsAdapter != null) {
                songsAdapter.playRandom(v);
            } else {
                Toast.makeText(context, "Initializing player! Please wait...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void refresh() {

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaBrowser.disconnect();
    }


    void artistTracks() {
        viewModel.loadArtists(mMediaBrowser, parentId);
        viewModel.allArtists.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
            @Override
            public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                binding.listProgress.setVisibility(View.GONE);
                SongsListAdapter adapter = new SongsListAdapter(context, launcher, songs, new SongListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(View v, MediaBrowserCompat.MediaItem song) {
                        if (song.isBrowsable())
                            viewModel.loadArtists(mMediaBrowser, song.getMediaId());
                        else if (song.isPlayable())
                            mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                    }
                }, new SongsContextMenuClickListener(context, mMediaController), SongsListAdapter.MODE.ARTIST);
                adapter.setViewModel(viewModel);
                binding.listRv.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                binding.listRv.setAdapter(adapter);
            }
        });
    }

    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
                }
            });

    void albumTracks() {
        String[] parts = parentId.split("[/]");
        Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), Long.parseLong(parts[parts.length - 1]))).placeholder(R.drawable.album_art_placeholder).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.appBarImage);
        viewModel.loadAlbums(mMediaBrowser, parentId);
        viewModel.allAlbums.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
            @Override
            public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                binding.listProgress.setVisibility(View.GONE);
                SongsListAdapter adapter = new SongsListAdapter(context, launcher, songs, new SongListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(View v, MediaBrowserCompat.MediaItem song) {
                        if (song.isBrowsable())
                            viewModel.loadAlbums(mMediaBrowser, song.getMediaId());
                        else if (song.isPlayable())
                            mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                    }
                }, new SongsContextMenuClickListener(context, mMediaController), SongsListAdapter.MODE.ALBUM);
                adapter.setViewModel(viewModel);
                binding.listRv.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                binding.listRv.setAdapter(adapter);
                binding.listRv.addItemDecoration(new DividerItemDecoration(ListExpandActivity.this, DividerItemDecoration.VERTICAL));
            }
        });
    }


    void playListTracks() {
        viewModel.loadPlaylists(mMediaBrowser, parentId);
        viewModel.allPlaylists.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
            @Override
            public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                binding.listProgress.setVisibility(View.GONE);
                SongsListAdapter adapter = new SongsListAdapter(context, launcher, songs, new SongListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(View v, MediaBrowserCompat.MediaItem song) {
                        if (song.isBrowsable())
                            viewModel.loadPlaylists(mMediaBrowser, song.getMediaId());
                        else if (song.isPlayable())
                            mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                    }
                }, new SongsContextMenuClickListener(context, mMediaController), SongsListAdapter.MODE.PLAYLIST);
                adapter.setViewModel(viewModel);
                binding.listRv.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                binding.listRv.setAdapter(adapter);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }

    }

}
