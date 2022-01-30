package com.yash.ymplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.ymplayer.databinding.ActivitySearchBinding;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.ui.main.SearchViewModel;
import com.yash.ymplayer.util.SearchListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends BasePlayerActivity {
    private static final String TAG = "debug";
    public static final String SONGS_TAG = "songs";
    public static final String ARTISTS_TAG = "artists";
    public static final String ALBUMS_TAG = "albums";
    ActivitySearchBinding searchBinding;
    SearchView searchView;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    SearchViewModel viewModel;

    SearchListAdapter searchListSongAdapter;
    SearchListAdapter searchListAlbumAdapter;
    SearchListAdapter searchListArtistAdapter;

    List<List<MediaBrowserCompat.MediaItem>> lists = null;
    ExecutorService executorService;
    String searchQuery = "";
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, MediaBrowserCompat mediaBrowser, BasePlayerActivityBinding playerActivityBinding) {
        searchBinding = ActivitySearchBinding.inflate(getLayoutInflater());
        playerActivityBinding.container.addView(searchBinding.getRoot());
        setCustomToolbar(searchBinding.toolbar, "Search");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long duration = 5000;
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        this.mediaBrowser = mediaBrowser;
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1);

        searchListSongAdapter = new SearchListAdapter(this, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isPlayable()) {
                    searchView.clearFocus();
                    mediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                }
            }
        }, SearchListAdapter.ItemType.SONGS, searchBinding.songsHeading, searchBinding.searchListSongsContainer);
        searchListSongAdapter.setHasStableIds(true);
        searchBinding.searchListSongsContainer.setLayoutManager(new LinearLayoutManager(this));
        searchBinding.searchListSongsContainer.setNestedScrollingEnabled(false);
//        searchBinding.searchListSongsContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        searchBinding.searchListSongsContainer.setHasFixedSize(true);
        searchBinding.searchListSongsContainer.setItemViewCacheSize(20);
        searchBinding.searchListSongsContainer.setAdapter(searchListSongAdapter);

        searchListAlbumAdapter = new SearchListAdapter(this, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song, long id) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(SearchActivity.this, ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "album");
                    intent.putExtra("imageId", id);
                    startActivity(intent);
//                    finish();
                }
            }
        }, SearchListAdapter.ItemType.ALBUMS, searchBinding.albumsHeading, searchBinding.searchListAlbumsContainer);
//        searchListAlbumAdapter.setHasStableIds(true);

        searchBinding.searchListAlbumsContainer.setLayoutManager(new GridLayoutManager(this, 2));
        searchBinding.searchListAlbumsContainer.setNestedScrollingEnabled(false);
        searchBinding.searchListAlbumsContainer.setHasFixedSize(true);
        searchBinding.searchListAlbumsContainer.setItemViewCacheSize(20);
        searchBinding.searchListAlbumsContainer.setAdapter(searchListAlbumAdapter);

        searchListArtistAdapter = new SearchListAdapter(this, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(SearchActivity.this, ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "artist");
                    startActivity(intent);
//                    finish();
                }
            }
        }, SearchListAdapter.ItemType.SONGS, searchBinding.artistsHeading, searchBinding.searchListArtistsContainer);
        searchBinding.searchListArtistsContainer.setLayoutManager(new LinearLayoutManager(this));
        searchBinding.searchListArtistsContainer.setNestedScrollingEnabled(false);
        searchBinding.searchListArtistsContainer.setHasFixedSize(true);
        searchBinding.searchListArtistsContainer.setItemViewCacheSize(20);
        searchBinding.searchListArtistsContainer.setAdapter(searchListArtistAdapter);

        searchBinding.searchListSongsContainer.setVisibility(View.GONE);
        searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
        searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
        searchBinding.songsHeading.setVisibility(View.GONE);
        searchBinding.albumsHeading.setVisibility(View.GONE);
        searchBinding.artistsHeading.setVisibility(View.GONE);
    }

    @Override
    protected void onConnected(MediaControllerCompat mediaController) {
        this.mediaController = mediaController;
        viewModel.refreshSearchData(SearchActivity.this, mediaBrowser);
        viewModel.allSearchData.observe(SearchActivity.this, new Observer<List<List<MediaBrowserCompat.MediaItem>>>() {
            @Override
            public void onChanged(List<List<MediaBrowserCompat.MediaItem>> lists) {
                SearchActivity.this.lists = lists;
            }
        });
    }

    @Override
    public void refresh() {

    }


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
                SearchActivity.this.finish();
                return true;
            }
        });
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchListSongAdapter == null || searchListArtistAdapter == null || searchListAlbumAdapter == null)
                    return false;
                handler.removeCallbacks(null);
                final String query = newText.toLowerCase();
//                searchQuery = query;
                if (query.isEmpty()) {
                    searchListSongAdapter.updateList(new ArrayList<>());
                    searchListAlbumAdapter.updateList(new ArrayList<>());
                    searchListArtistAdapter.updateList(new ArrayList<>());
                    searchBinding.searchListSongsContainer.setVisibility(View.GONE);
                    searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
                    searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
                    searchBinding.songsHeading.setVisibility(View.GONE);
                    searchBinding.albumsHeading.setVisibility(View.GONE);
                    searchBinding.artistsHeading.setVisibility(View.GONE);
                    return true;
                }


                handler.post(new RunnableWithParams(query) {
                    @Override
                    public void run(String text) {
                        executorService.execute(new RunnableWithParams(text) {
                            @Override
                            public void run(String text) {
                                final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                                for (MediaBrowserCompat.MediaItem model : lists.get(0)) {
                                    final String title = model.getDescription().getTitle().toString().toLowerCase();
                                    if (title.contains(text)) {
                                        filteredModelList.add(model);
                                    }
                                }
                                handler.post(() -> {
                                    if (filteredModelList.isEmpty()) {
                                        searchBinding.searchListSongsContainer.setVisibility(View.GONE);
                                        searchBinding.songsHeading.setVisibility(View.GONE);
                                    } else {
                                        searchBinding.searchListSongsContainer.setVisibility(View.VISIBLE);
                                        searchBinding.songsHeading.setVisibility(View.VISIBLE);
                                    }
                                    searchListSongAdapter.updateList(filteredModelList);
                                });
                            }
                        });
                        executorService.execute(new RunnableWithParams(text) {
                            @Override
                            public void run(String text) {
                                final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                                for (MediaBrowserCompat.MediaItem model : lists.get(2)) {
                                    final String title = model.getDescription().getTitle().toString().toLowerCase();
                                    if (!title.isEmpty() && title.contains(text)) {
                                        filteredModelList.add(model);
                                    }
                                }
                                handler.post(() -> {
                                    if (filteredModelList.isEmpty()) {
                                        searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
                                        searchBinding.artistsHeading.setVisibility(View.GONE);
                                    } else {
                                        searchBinding.searchListArtistsContainer.setVisibility(View.VISIBLE);
                                        searchBinding.artistsHeading.setVisibility(View.VISIBLE);
                                    }
                                    searchListArtistAdapter.updateList(filteredModelList);
                                });
                            }
                        });
                        executorService.execute(new RunnableWithParams(text) {
                            @Override
                            public void run(String text) {
                                final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                                for (MediaBrowserCompat.MediaItem model : lists.get(1)) {
                                    final String title = model.getDescription().getTitle().toString().toLowerCase();
                                    if (title.contains(text)) {
                                        filteredModelList.add(model);
                                    }
                                }
                                handler.post(() -> {
                                    if (filteredModelList.isEmpty()) {
                                        searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
                                        searchBinding.albumsHeading.setVisibility(View.GONE);
                                    } else {
                                        searchBinding.searchListAlbumsContainer.setVisibility(View.VISIBLE);
                                        searchBinding.albumsHeading.setVisibility(View.VISIBLE);
                                    }
                                    searchListAlbumAdapter.updateList(filteredModelList);
                                });
                            }
                        });
                    }
                });

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

        });
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    abstract static class RunnableWithParams implements Runnable {
        String text;

        public RunnableWithParams(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            run(text);
        }

        public abstract void run(String text);
    }
}
