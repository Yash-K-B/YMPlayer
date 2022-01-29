package com.yash.ymplayer.ui.youtube.search;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.R;
import com.yash.ymplayer.SearchActivity;
import com.yash.ymplayer.databinding.ActivitySearchBinding;
import com.yash.ymplayer.databinding.ActivityUtubeSearchBinding;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.ui.youtube.YoutubeTracksAdapter;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;

public class YoutubeSearch extends BaseActivity {
    private static final String TAG = "YoutubeSearch";
    ActivityUtubeSearchBinding utubeSearchBinding;
    SearchView searchView;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utubeSearchBinding = ActivityUtubeSearchBinding.inflate(getLayoutInflater());
        setContentView(utubeSearchBinding.getRoot());
        setSupportActionBar(utubeSearchBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long duration = 5000;
        utubeSearchBinding.progressBar.setVisibility(View.INVISIBLE);
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();

    }

    @Override
    public void refresh() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaBrowser.disconnect();
    }


    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            try {
                mediaController = new MediaControllerCompat(YoutubeSearch.this, mediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_search_menu, menu);
        MenuItem item = menu.findItem(R.id.search_menu);
        item.expandActionView();
        searchView = (SearchView) item.getActionView();
        searchView.setIconified(false);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                YoutubeSearch.this.finish();
                return true;
            }
        });
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                utubeSearchBinding.progressBar.setVisibility(View.VISIBLE);
                LogHelper.d(TAG, "onQueryTextSubmit: " + query);
                OnlineYoutubeRepository.getInstance(YoutubeSearch.this).searchTracks(query, new OnlineYoutubeRepository.TracksLoadedCallback() {
                    @Override
                    public void onLoaded(List<YoutubeSong> songs) {
                        LogHelper.d(TAG, "onLoaded: Youtube search songs" + songs);
                        runOnUiThread(() -> {
                            YoutubeTracksAdapter adapter = new YoutubeTracksAdapter(YoutubeSearch.this, songs, new YoutubeTracksAdapter.TrackClickListener() {
                                @Override
                                public void onClick(YoutubeSong song) {
                                    String id = "Search/" + query + "|" + song.getVideoId();
                                    LogHelper.d(TAG, "onClick: uri" + song.getVideoId());
                                    if (mediaController != null)
                                        mediaController.getTransportControls().playFromUri(Uri.parse(id), null);
                                }
                            });
                            utubeSearchBinding.progressBar.setVisibility(View.INVISIBLE);
                            utubeSearchBinding.searchResultContainer.setLayoutManager(new LinearLayoutManager(YoutubeSearch.this));
                            utubeSearchBinding.searchResultContainer.setAdapter(adapter);
                        });
                    }

                    @Override
                    public void onError() {
                        LogHelper.d(TAG, "onError: ");
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}
