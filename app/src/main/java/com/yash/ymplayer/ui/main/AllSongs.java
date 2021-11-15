package com.yash.ymplayer.ui.main;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAllSongsBinding;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsContextMenuClickListener;
import com.yash.ymplayer.util.SongsListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllSongs extends Fragment {
    private static final String TAG = "debug";
    Context context;
    FragmentActivity activity;
    MediaBrowserCompat mMediaBrowser;
    MediaControllerCompat mMediaController;
    LocalViewModel viewModel;
    FragmentAllSongsBinding allSongsBinding;
    List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    SongsListAdapter songsAdapter;
    private static AllSongs instance;

    public AllSongs() {
        // Required empty public constructor
    }

    public static AllSongs getInstance() {
        if (instance == null)
            instance = new AllSongs();
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        allSongsBinding = FragmentAllSongsBinding.inflate(inflater, container, false);
        return allSongsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        activity = getActivity();
        mMediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        viewModel = new ViewModelProvider(activity).get(LocalViewModel.class);
        allSongsBinding.allSongsView.setHasFixedSize(true);
        allSongsBinding.allSongsView.setItemViewCacheSize(20);
        allSongsBinding.allSongsView.setLayoutManager(new LinearLayoutManager(context));
        allSongsBinding.allSongsView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));


    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Allsongs");
        // mMediaBrowser.disconnect();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mMediaBrowser.disconnect();
    }

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after {@link MediaBrowser#connect()} when the request has successfully completed.
         */
        @Override
        public void onConnected() {
            try {
                songs.clear();
                mMediaController = new MediaControllerCompat(context, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallbacks);
                songsAdapter = new SongsListAdapter(context,launcher, songs, new SongListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(View v, MediaBrowserCompat.MediaItem song) {
                        mMediaController.getTransportControls().playFromMediaId(song.getDescription().getMediaId(), null);
                        Log.d(TAG, "onClick: Extra: null");
                    }
                }, new SongsContextMenuClickListener(context, mMediaController), SongsListAdapter.MODE.ALL);
                songsAdapter.setViewModel(viewModel);
                allSongsBinding.allSongsView.setAdapter(songsAdapter);
                allSongsBinding.allSongsRefresh.setColorSchemeColors(BaseActivity.getAttributeColor(context, R.attr.colorPrimary));
                allSongsBinding.allSongsRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        allSongsBinding.allSongsRefresh.setRefreshing(true);
                        viewModel.refresh(getContext(), mMediaBrowser);
                        //viewModel.querySongs();
                    }
                });
                if (viewModel.songs.getValue() == null || viewModel.songs.getValue().isEmpty())
                    viewModel.querySongs(mMediaBrowser);
                viewModel.songs.observe(AllSongs.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        AllSongs.this.songs.clear();
                        AllSongs.this.songs.addAll(songs);
                        Log.d(TAG, "onChanged: Song Refreshed");
                        allSongsBinding.allSongsRefresh.setRefreshing(false);
                        allSongsBinding.progressBar.setVisibility(View.INVISIBLE);
                        songsAdapter.refreshList();
                        songsAdapter.notifyDataSetChanged();
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };

    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
                }
            });

    public MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
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
