package com.yash.ymplayer.ui.main;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAllSongsBinding;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.Song;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllSongs extends Fragment {
    private static final String TAG = "debug";
    Context context;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private LocalViewModel viewModel;
    private FragmentAllSongsBinding allSongsBinding;
    private List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    private SongsListAdapter adapter;
    Handler handler = new Handler();
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
        mMediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, PlayerService.class), mConnectionCallbacks, null);
        mMediaBrowser.connect();
        viewModel = new ViewModelProvider(getActivity()).get(LocalViewModel.class);
        allSongsBinding.allSongsView.setHasFixedSize(true);
        allSongsBinding.allSongsView.setItemViewCacheSize(20);
        allSongsBinding.allSongsView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new SongsListAdapter(context, songs, new SongListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                mMediaController.getTransportControls().playFromMediaId(song.getDescription().getMediaId(), null);
                Log.d(TAG, "onClick: Extra: null");
            }
        }, songContextMenuListener,SongsListAdapter.MODE.ALL);
        allSongsBinding.allSongsView.setAdapter(adapter);
        allSongsBinding.allSongsView.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.VERTICAL));


    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Allsongs");
        mMediaBrowser.disconnect();
    }

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after {@link MediaBrowser#connect()} when the request has successfully completed.
         */
        @Override
        public void onConnected() {
            try {
                mMediaController = new MediaControllerCompat(context, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallbacks);
                allSongsBinding.allSongsRefresh.setColorSchemeColors(((MainActivity)getActivity()).getAttributeColor(R.attr.colorPrimary));
                allSongsBinding.allSongsRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        allSongsBinding.allSongsRefresh.setRefreshing(true);
                        viewModel.refresh(getContext(),mMediaBrowser);
                        //viewModel.querySongs();
                    }
                });
                viewModel.querySongs(mMediaBrowser);
                viewModel.songs.observe(AllSongs.this, new Observer<List<MediaBrowserCompat.MediaItem>>() {
                    @Override
                    public void onChanged(List<MediaBrowserCompat.MediaItem> songs) {
                        AllSongs.this.songs.clear();
                        AllSongs.this.songs.addAll(songs);
                        Log.d(TAG, "onChanged: Song Refreshed");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                allSongsBinding.allSongsRefresh.setRefreshing(false);
                                adapter.notifyDataSetChanged();
                                allSongsBinding.progressBar.setVisibility(View.INVISIBLE);
                            }
                        },400);
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };

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
            Intent intent = new Intent(getActivity(), ListExpandActivity.class);
            intent.putExtra("parent_id", item.getDescription().getDescription());
            intent.putExtra("type", "album");
            intent.putExtra("imageId", Long.parseLong(Repository.getInstance(context).getOfflineProvider().getAlbumId(item.getMediaId())));
            startActivity(intent);
        }

        @Override
        public void gotoArtist(MediaBrowserCompat.MediaItem item) {
            Intent intent = new Intent(getActivity(), ListExpandActivity.class);
            intent.putExtra("parent_id", item.getDescription().getSubtitle());
            intent.putExtra("type", "artist");
            startActivity(intent);
        }
    };



}
