package com.yash.ymplayer.ui.main;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentPlaylistsBinding;
import com.yash.ymplayer.interfaces.PlaylistUpdateListener;
import com.yash.ymplayer.ui.custom.PlayerAware;
import com.yash.ymplayer.util.AlbumOrArtistContextMenuClickListener;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;

public class Playlists extends Fragment implements PlaylistUpdateListener, PlayerAware {
    private static final String TAG = "debug";
    private static Playlists instance;
    private MediaBrowserCompat mMediaBrowser;
    private CategoryAdapter adapter;
    private final List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    FragmentPlaylistsBinding playlistsBinding;
    private LocalViewModel viewModel;
    private MediaControllerCompat mMediaController;
    private final Handler handler = new Handler(Looper.getMainLooper());


    public Playlists() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        playlistsBinding = FragmentPlaylistsBinding.inflate(inflater, container, false);
        return playlistsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(), PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        adapter = new CategoryAdapter(getContext(), (v, song) -> {
            if (song.isBrowsable()) {
                Intent intent = new Intent(getActivity(), ListExpandActivity.class);
                intent.putExtra(Keys.EXTRA_PARENT_ID, song.getMediaId());
                intent.putExtra(Keys.EXTRA_TYPE, "playlist");
                intent.putExtra(Keys.EXTRA_TITLE, song.getDescription().getTitle());
                startActivity(intent);
            }
        }, new AlbumOrArtistContextMenuClickListener(getContext(), mMediaController), CategoryAdapter.Mode.PLAYLIST);
        playlistsBinding.listRv.setAdapter(adapter);
        playlistsBinding.listRv.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistsBinding.listRv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
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

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            try {
                viewModel = new ViewModelProvider(Playlists.this).get(LocalViewModel.class);
                mMediaController = new MediaControllerCompat(getContext(), mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallbacks);
                playlistsBinding.playlistRefresh.setColorSchemeColors(BaseActivity.getAttributeColor(requireContext(), R.attr.colorPrimary));
                playlistsBinding.playlistRefresh.setOnRefreshListener(() -> {
                    playlistsBinding.playlistRefresh.setRefreshing(true);
                    viewModel.loadPlaylists(mMediaBrowser, null);
                });
                viewModel.loadPlaylists(mMediaBrowser, null);
                viewModel.allPlaylists.observe(requireActivity(), songs -> {
                    Log.d(TAG, "onChanged: Playlist: " + songs.size());
                    initPlaylist();
                    Playlists.this.songs.addAll(songs);
                    adapter.refreshList(Playlists.this.songs);
                    playlistsBinding.noPlaylist.setVisibility((Playlists.this.songs.size() == 0) ? View.VISIBLE : View.INVISIBLE);
                    playlistsBinding.playlistRefresh.setRefreshing(false);
                    playlistsBinding.loading.setVisibility(View.INVISIBLE);
                });
                LocalSongs localSongs = (LocalSongs) Playlists.this.getParentFragment();
                localSongs.onFabClicked(Playlists.this);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    private final MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
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
            viewModel.loadPlaylists(mMediaBrowser, null);
    }


    void initPlaylist() {
        songs.clear();
//        songs.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
//                .setMediaId("PLAYLISTS/RECENT")
//                .setTitle("Recently Played")
//                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        songs.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId("PLAYLISTS/LAST_ADDED")
                .setTitle("Last Added")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        songs.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId("PLAYLISTS/FAVOURITE")
                .setTitle("Favourites")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        songs.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId("PLAYLISTS/LAST_PLAYED")
                .setTitle("Last Played")
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
    }
}
