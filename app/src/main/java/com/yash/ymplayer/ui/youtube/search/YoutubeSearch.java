package com.yash.ymplayer.ui.youtube.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.BasePlayerActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.R;
import com.yash.ymplayer.SearchActivity;
import com.yash.ymplayer.databinding.ActivitySearchBinding;
import com.yash.ymplayer.databinding.ActivityUtubeSearchBinding;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.ui.youtube.YoutubeTracksAdapter;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.StringUtil;
import com.yash.ymplayer.util.TrackContextMenuClickListener;
import com.yash.ymplayer.util.YoutubeSong;

import java.security.Key;
import java.util.List;

public class YoutubeSearch extends BasePlayerActivity {
    private static final String TAG = "YoutubeSearch";
    ActivityUtubeSearchBinding utubeSearchBinding;
    SearchView searchView;
    MediaControllerCompat mediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, MediaBrowserCompat mediaBrowser, BasePlayerActivityBinding playerActivityBinding) {
        utubeSearchBinding = ActivityUtubeSearchBinding.inflate(getLayoutInflater());
        playerActivityBinding.container.addView(utubeSearchBinding.getRoot());
        setCustomToolbar(null, "Search");
        long duration = 5000;
        utubeSearchBinding.progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onConnected(MediaControllerCompat mediaController) {
        this.mediaController = mediaController;
    }

    @Override
    public void refresh() {

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_search_menu, menu);
        MenuItem item = menu.findItem(R.id.search_menu);
        item.expandActionView();
        searchView = (SearchView) item.getActionView();
        searchView.setIconified(false);
        searchView.setOnCloseListener(() -> {
            YoutubeSearch.this.finish();
            return true;
        });
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!StringUtil.hasLength(query)) {
                    Toast.makeText(YoutubeSearch.this, "Please enter text to search", Toast.LENGTH_SHORT).show();
                }
                query = query.trim();
                String mediaIdPrefix = "Search/" + query + "|";
                utubeSearchBinding.progressBar.setVisibility(View.VISIBLE);
                LogHelper.d(TAG, "onQueryTextSubmit: " + query);
                OnlineYoutubeRepository.getInstance(YoutubeSearch.this).searchTracks(query, new OnlineYoutubeRepository.TracksLoadedCallback() {
                    @Override
                    public void onLoaded(List<YoutubeSong> songs) {
                        LogHelper.d(TAG, "onLoaded: Youtube search songs" + songs);
                        runOnUiThread(() -> {
                            YoutubeTracksAdapter adapter = new YoutubeTracksAdapter(YoutubeSearch.this, songs, new TrackContextMenuClickListener(YoutubeSearch.this, mediaController, mediaIdPrefix));
                            utubeSearchBinding.progressBar.setVisibility(View.INVISIBLE);
                            utubeSearchBinding.searchResultContainer.setLayoutManager(new LinearLayoutManager(YoutubeSearch.this));
                            utubeSearchBinding.searchResultContainer.setAdapter(adapter);
                        });
                    }

                    @Override
                    public <E extends Exception> void onError(E e) {
                        LogHelper.e(TAG, "onError: ", e);
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        String query = getIntent().getStringExtra("query");
        if (StringUtil.hasText(query)) {
            searchView.setQuery(query, true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
