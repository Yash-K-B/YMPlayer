package com.yash.ymplayer;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.yash.ymplayer.databinding.ListExpandActivityBinding;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.main.LocalViewModel;
import com.yash.ymplayer.ui.main.SongContextMenuListener;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsListAdapter;

import java.util.List;

public class ListExpandActivity extends BaseActivity {
    private static final String TAG = "debug";
    ListExpandActivityBinding binding;
    private MediaBrowserCompat mMediaBrowser;
    private LocalViewModel viewModel;
    private MediaControllerCompat mMediaController;
    String type = null;
    String parentId;
    long albumId;
    Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ListExpandActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(ListExpandActivity.this).get(LocalViewModel.class);
        mMediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        parentId = getIntent().getStringExtra("parent_id");
        type = getIntent().getStringExtra("type");
        albumId = getIntent().getLongExtra("imageId", 0);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(parentId);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;
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

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            if (type.equalsIgnoreCase("artist")) {
                artistTracks();
            } else if (type.equalsIgnoreCase("album")) {
                albumTracks();
            }
            else if(type.equalsIgnoreCase("playlist")){
                playListTracks();
            }
        }
    };

    void artistTracks() {
        try {
            mMediaController = new MediaControllerCompat(ListExpandActivity.this, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mMediaControllerCallbacks);
            viewModel.getAllArtists(mMediaBrowser, parentId);
            viewModel.allArtists.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                @Override
                public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                    SongsListAdapter adapter = new SongsListAdapter(context, songs, new SongListAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(MediaBrowserCompat.MediaItem song) {
                            if (song.isBrowsable())
                                viewModel.getAllArtists(mMediaBrowser, song.getMediaId());
                            else if (song.isPlayable())
                                mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                        }
                    }, songContextMenuListener, SongsListAdapter.MODE.ARTIST);
                    binding.list.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                    binding.list.setAdapter(adapter);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    void albumTracks() {
        try {
            Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)).placeholder(R.drawable.album_art_placeholder).into(binding.appBarImage);
            mMediaController = new MediaControllerCompat(ListExpandActivity.this, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mMediaControllerCallbacks);
            viewModel.getAllAlbums(mMediaBrowser, parentId);
            viewModel.allAlbums.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                @Override
                public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                    SongsListAdapter adapter = new SongsListAdapter(context, songs, new SongListAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(MediaBrowserCompat.MediaItem song) {
                            if (song.isBrowsable())
                                viewModel.getAllAlbums(mMediaBrowser, song.getMediaId());
                            else if (song.isPlayable())
                                mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                        }
                    }, songContextMenuListener, SongsListAdapter.MODE.ALBUM);
                    binding.list.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                    binding.list.setAdapter(adapter);
                    binding.list.addItemDecoration(new DividerItemDecoration(ListExpandActivity.this,DividerItemDecoration.VERTICAL));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void playListTracks(){
        try {
            mMediaController = new MediaControllerCompat(ListExpandActivity.this, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mMediaControllerCallbacks);
            viewModel.getAllPlaylists(mMediaBrowser, parentId);
            viewModel.allPlaylists.observe(ListExpandActivity.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                @Override
                public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                    SongsListAdapter adapter = new SongsListAdapter(context, songs, new SongListAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(MediaBrowserCompat.MediaItem song) {
                            if (song.isBrowsable())
                                viewModel.getAllPlaylists(mMediaBrowser, song.getMediaId());
                            else if (song.isPlayable())
                                mMediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                        }
                    }, songContextMenuListener, SongsListAdapter.MODE.ALL);
                    binding.list.setLayoutManager(new LinearLayoutManager(ListExpandActivity.this));
                    binding.list.setAdapter(adapter);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
        }
    };

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


    SongContextMenuListener songContextMenuListener = new SongContextMenuListener() {
        @Override
        public void playSingle(MediaBrowserCompat.MediaItem item) {
            Bundle extra = new Bundle();
            extra.putBoolean(Keys.PLAY_SINGLE,true);
            mMediaController.getTransportControls().playFromMediaId(item.getDescription().getMediaId(), extra);
        }

        @Override
        public void queueNext(MediaBrowserCompat.MediaItem item) {

            Bundle extras = new Bundle();
            extras.putString(Keys.MEDIA_ID,item.getDescription().getMediaId());
            mMediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT,extras);
        }

        @Override
        public void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist) {
            String[] parts = item.getDescription().getMediaId().split("[/|]");
            Log.d(TAG, "addToPlaylist: id:"+parts[parts.length-1]);
            Bundle extras = new Bundle();
            extras.putString(Keys.MEDIA_ID,parts[parts.length - 1]);
            extras.putString(Keys.TITLE,item.getDescription().getTitle().toString());
            extras.putString(Keys.ARTIST,item.getDescription().getSubtitle().toString());
            extras.putString(Keys.ALBUM,item.getDescription().getDescription().toString());
            extras.putString(Keys.PLAYLIST_NAME,playlist);
            mMediaController.getTransportControls().sendCustomAction(Keys.Action.ADD_TO_PLAYLIST,extras);
        }

        @Override
        public void gotoAlbum(MediaBrowserCompat.MediaItem item) {
            Intent intent = new Intent(ListExpandActivity.this, ListExpandActivity.class);
            intent.putExtra("parent_id", item.getDescription().getDescription());
            intent.putExtra("type", "album");
            intent.putExtra("imageId", Long.parseLong(Repository.getInstance(ListExpandActivity.this).getOfflineProvider().getAlbumId(item.getMediaId())));
            startActivity(intent);
        }

        @Override
        public void gotoArtist(MediaBrowserCompat.MediaItem item) {
            Intent intent = new Intent(ListExpandActivity.this, ListExpandActivity.class);
            intent.putExtra("parent_id", item.getDescription().getSubtitle());
            intent.putExtra("type", "artist");
            startActivity(intent);
        }
    };
}
