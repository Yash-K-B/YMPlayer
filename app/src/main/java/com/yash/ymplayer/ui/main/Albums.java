package com.yash.ymplayer.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAlbumsBinding;
import com.yash.ymplayer.util.AlbumListAdapter;
import com.yash.ymplayer.util.AlbumOrArtistContextMenuClickListener;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.MarginItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Albums extends Fragment {

    MediaBrowserCompat mMediaBrowser;
    MediaControllerCompat mMediaController;
    LocalViewModel viewModel;
    FragmentAlbumsBinding albumsBinding;
    List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    AlbumListAdapter albumsAdapter;
    private static Albums instance;
    Context context;
    FragmentActivity activity;

    public Albums() {
        // Required empty public constructor
    }

    public static Albums getInstance() {
        if (instance == null)
            instance = new Albums();
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        albumsBinding = FragmentAlbumsBinding.inflate(inflater, container, false);
        return albumsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        activity = getActivity();

        albumsBinding.allAlbums.setHasFixedSize(true);
        albumsBinding.allAlbums.setItemViewCacheSize(20);
        albumsBinding.allAlbums.addItemDecoration(new MarginItemDecoration(10));
        albumsBinding.allAlbums.setLayoutManager(new GridLayoutManager(context,3));

        mMediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
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
                viewModel = new ViewModelProvider(activity).get(LocalViewModel.class);
                mMediaController = new MediaControllerCompat(getContext(), mMediaBrowser.getSessionToken());
                albumsAdapter = new AlbumListAdapter(context, songs, (song) -> {
                    if (song.isBrowsable()) {
                        Intent intent = new Intent(getActivity(), ListExpandActivity.class);
                        intent.putExtra(Keys.EXTRA_PARENT_ID, song.getMediaId());
                        intent.putExtra(Keys.EXTRA_TYPE, "album");
                        intent.putExtra(Keys.EXTRA_TITLE, song.getDescription().getTitle());
                        startActivity(intent);
                    }
                },new AlbumOrArtistContextMenuClickListener(context,mMediaController));
                albumsBinding.allAlbums.setAdapter(albumsAdapter);
                albumsBinding.albumRefresh.setColorSchemeColors(BaseActivity.getAttributeColor(context,R.attr.colorPrimary));
                albumsBinding.albumRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        albumsBinding.albumRefresh.setRefreshing(true);
                        viewModel.refresh(context, mMediaBrowser);
                        //viewModel.getAllAlbums(, null);
                    }
                });
                if (viewModel.allAlbums.getValue() == null || viewModel.allAlbums.getValue().isEmpty())
                    viewModel.getAllAlbums(mMediaBrowser, null);
                viewModel.allAlbums.observe(activity, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        Albums.this.songs.clear();
                        Albums.this.songs.addAll(songs);
                        albumsBinding.albumRefresh.setRefreshing(false);
                        albumsAdapter.refreshList();
                        albumsAdapter.notifyDataSetChanged();
                        albumsBinding.progressBar.setVisibility(View.INVISIBLE);

                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }


    };

//    MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
//        @Override
//        public void onPlaybackStateChanged(PlaybackStateCompat state) {
//            super.onPlaybackStateChanged(state);
//        }
//
//        @Override
//        public void onMetadataChanged(MediaMetadataCompat metadata) {
//            super.onMetadataChanged(metadata);
//        }
//
//        @Override
//        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
//            super.onAudioInfoChanged(info);
//        }
//    };

}
