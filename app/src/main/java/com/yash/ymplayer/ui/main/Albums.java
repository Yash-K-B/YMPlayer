package com.yash.ymplayer.ui.main;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAlbumsBinding;
import com.yash.ymplayer.util.AlbumListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A simple {@link Fragment} subclass.
 */
public class Albums extends Fragment {

    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private LocalViewModel viewModel;
    private FragmentAlbumsBinding albumsBinding;
    private List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    private AlbumListAdapter adapter;
    private static Albums instance;
    private Handler handler = new Handler();

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(), PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();

        adapter = new AlbumListAdapter(getContext(), songs, new AlbumListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song,long id) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(getActivity(), ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "album");
                    intent.putExtra("imageId",id);
                    startActivity(intent);
                }
            }
        });
        albumsBinding.allAlbums.setHasFixedSize(true);
        albumsBinding.allAlbums.setItemViewCacheSize(20);
        albumsBinding.allAlbums.setLayoutManager(new GridLayoutManager(getContext(), getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT?2:3));
        albumsBinding.allAlbums.setAdapter(adapter);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
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
                viewModel = new ViewModelProvider(getActivity()).get(LocalViewModel.class);
                mMediaController = new MediaControllerCompat(getContext(), mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallbacks);
                albumsBinding.albumRefresh.setColorSchemeColors(((MainActivity)getActivity()).getAttributeColor(R.attr.colorPrimary));
                albumsBinding.albumRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        albumsBinding.albumRefresh.setRefreshing(true);
                        viewModel.refresh(getContext(),mMediaBrowser);
                        //viewModel.getAllAlbums(, null);
                    }
                });
                viewModel.getAllAlbums(mMediaBrowser, null);
                viewModel.allAlbums.observe(getActivity(), new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        Albums.this.songs.clear();
                        Albums.this.songs.addAll(songs);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                albumsBinding.albumRefresh.setRefreshing(false);
                                adapter.notifyDataSetChanged();
                                albumsBinding.progressBar.setVisibility(View.INVISIBLE);
                            }
                        },400);
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

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

}
