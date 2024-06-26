package com.yash.ymplayer.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentArtistsBinding;
import com.yash.ymplayer.util.AlbumOrArtistContextMenuClickListener;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Artists extends Fragment {
    FragmentArtistsBinding artistsBinding;
    MediaBrowserCompat mMediaBrowser;
    LocalViewModel viewModel;
    MediaControllerCompat mMediaController;
    CategoryAdapter artistsAdapter;
    private static Artists instance;
    Context context;
    FragmentActivity activity;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        activity = getActivity();
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(context, PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        artistsBinding.listRv.setLayoutManager(new LinearLayoutManager(getContext()));
        artistsBinding.listRv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
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

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            viewModel = new ViewModelProvider(activity).get(LocalViewModel.class);
            mMediaController = new MediaControllerCompat(getContext(), mMediaBrowser.getSessionToken());
            artistsAdapter = new CategoryAdapter(getContext(), new CategoryAdapter.OnItemClickListener() {
                @Override
                public void onClick(View v, MediaBrowserCompat.MediaItem song) {
                    if (song.isBrowsable()) {
                        Intent intent = new Intent(getActivity(), ListExpandActivity.class);
                        intent.putExtra(Keys.EXTRA_PARENT_ID, song.getMediaId());
                        intent.putExtra(Keys.EXTRA_TYPE, "artist");
                        intent.putExtra(Keys.EXTRA_TITLE, song.getDescription().getTitle());
                        startActivity(intent);
                    }
                }
            }, new AlbumOrArtistContextMenuClickListener(context, mMediaController), CategoryAdapter.Mode.ARTIST);
            artistsBinding.listRv.setAdapter(artistsAdapter);
            mMediaController.registerCallback(mMediaControllerCallbacks);
            artistsBinding.artistsRefresh.setColorSchemeColors(BaseActivity.getAttributeColor(context, R.attr.colorPrimary));
            artistsBinding.artistsRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    artistsBinding.artistsRefresh.setRefreshing(true);
                    viewModel.refresh(getContext(), mMediaBrowser);
                    //viewModel.getAllArtists(, null);
                }
            });
            if (viewModel.allArtists.getValue() == null || viewModel.allArtists.getValue().isEmpty())
                viewModel.loadArtists(mMediaBrowser, null);
            viewModel.allArtists.observe(requireActivity(), songs -> {
                artistsBinding.artistsRefresh.setRefreshing(false);
                artistsAdapter.refreshList(songs);
                artistsBinding.progressBar.setVisibility(View.INVISIBLE);
            });

        }
    };

    MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
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
