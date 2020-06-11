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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentArtistsBinding;
import com.yash.ymplayer.util.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Artists extends Fragment {
    private FragmentArtistsBinding artistsBinding;
    private MediaBrowserCompat mMediaBrowser;
    private LocalViewModel viewModel;
    private MediaControllerCompat mMediaController;
    private List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    private SongListAdapter adapter;
    private static Artists instance;
    private Handler handler = new Handler();

    public Artists() {
        // Required empty public constructor
    }

    public static Artists getInstance() {

        instance = new Artists();
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        artistsBinding = FragmentArtistsBinding.inflate(inflater, container, false);
        return artistsBinding.getRoot();
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
                    intent.putExtra("type", "artist");
                    startActivity(intent);
                }
            }
        }, 1);
        artistsBinding.allArtists.setAdapter(adapter);
        artistsBinding.allArtists.setLayoutManager(new LinearLayoutManager(getContext()));
        artistsBinding.allArtists.addItemDecoration(new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL));
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
                artistsBinding.artistsRefresh.setColorSchemeColors(((MainActivity)getActivity()).getAttributeColor(R.attr.colorPrimary));
                artistsBinding.artistsRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        artistsBinding.artistsRefresh.setRefreshing(true);
                        viewModel.refresh(getContext(),mMediaBrowser);
                        //viewModel.getAllArtists(, null);
                    }
                });
                viewModel.getAllArtists(mMediaBrowser, null);
                viewModel.allArtists.observe(getActivity(), new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        Artists.this.songs.clear();
                        Artists.this.songs.addAll(songs);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                artistsBinding.artistsRefresh.setRefreshing(false);
                                adapter.notifyDataSetChanged();
                                artistsBinding.progressBar.setVisibility(View.INVISIBLE);
                            }
                        }, 400);
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
