package com.yash.ymplayer.ui.main;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentPlaylistsBinding;
import com.yash.ymplayer.util.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

public class Playlists extends Fragment implements PlaylistUpdateListener {
    private static final String TAG = "debug";
    private static Playlists instance;
    private MediaBrowserCompat mMediaBrowser;
    private SongListAdapter adapter;
    private List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    FragmentPlaylistsBinding playlistsBinding;
    private LocalViewModel viewModel;
    private MediaControllerCompat mMediaController;
    private Handler handler = new Handler();


    public Playlists() {
        // Required empty public constructor
    }

    public static Playlists getInstance() {

        instance = new Playlists();
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        playlistsBinding = FragmentPlaylistsBinding.inflate(inflater, container, false);
        return playlistsBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(), PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        adapter = new SongListAdapter(getContext(), songs, new SongListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(getActivity(), ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "playlist");
                    startActivity(intent);
                }
            }
        }, 2);
        playlistsBinding.playlists.setAdapter(adapter);
        playlistsBinding.playlists.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistsBinding.playlists.addItemDecoration(new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL));
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaBrowser.disconnect();
    }

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            try {
                viewModel = new ViewModelProvider(Playlists.this).get(LocalViewModel.class);
                mMediaController = new MediaControllerCompat(getContext(), mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallbacks);
                playlistsBinding.playlistRefresh.setColorSchemeColors(((MainActivity)getActivity()).getAttributeColor(R.attr.colorPrimary));
                playlistsBinding.playlistRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        playlistsBinding.playlistRefresh.setRefreshing(true);
                        viewModel.getAllPlaylists(mMediaBrowser, null);
                    }
                });
                viewModel.getAllPlaylists(mMediaBrowser, null);
                viewModel.allPlaylists.observe(getActivity(), new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        Log.d(TAG, "onChanged: Playlist: " + songs.size());
                        Playlists.this.songs.clear();
                        Playlists.this.songs.addAll(songs);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                playlistsBinding.noPlaylist.setVisibility((songs.size() == 0)?View.VISIBLE:View.INVISIBLE);
                                playlistsBinding.playlistRefresh.setRefreshing(false);
                                playlistsBinding.loading.setVisibility(View.INVISIBLE);
                            }
                        }, 400);
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            LocalSongs localSongs = (LocalSongs)Playlists.this.getParentFragment();
            localSongs.onFabClicked(Playlists.this);
        }
    };

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
    public void onChanged() {
        if (mMediaBrowser.isConnected())
            viewModel.getAllPlaylists(mMediaBrowser, null);
    }

}
