package com.yash.ymplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.ymplayer.databinding.ActivitySearchBinding;
import com.yash.ymplayer.ui.main.LocalViewModel;
import com.yash.ymplayer.ui.main.SearchViewModel;
import com.yash.ymplayer.util.SearchListAdapter;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends BaseActivity {
    private static final String TAG = "debug";
    ActivitySearchBinding searchBinding;
    SearchView searchView;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    SearchViewModel viewModel;
    SearchListAdapter searchListSongAdapter;
    SearchListAdapter searchListAlbumAdapter;
    SearchListAdapter searchListArtistAdapter;
    List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
    List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();
    List<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();
    ExecutorService executorService;
    Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchBinding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(searchBinding.getRoot());
        setSupportActionBar(searchBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long duration = 5000;
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1);

        searchListSongAdapter = new SearchListAdapter(this, songs, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isPlayable()) {
                    searchView.clearFocus();
                    mediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                    finish();
                }
            }
        }, SearchListAdapter.ItemType.SONGS, searchBinding.songsHeading, searchBinding.searchListSongsContainer);
//        searchListSongAdapter.setHasStableIds(true);
        searchBinding.searchListSongsContainer.setLayoutManager(new LinearLayoutManager(this));
        searchBinding.searchListSongsContainer.setNestedScrollingEnabled(false);
//        searchBinding.searchListSongsContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        searchBinding.searchListSongsContainer.setHasFixedSize(true);
        searchBinding.searchListSongsContainer.setItemViewCacheSize(20);
        searchBinding.searchListSongsContainer.setAdapter(searchListSongAdapter);

        searchListAlbumAdapter = new SearchListAdapter(this, albums, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song, long id) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(SearchActivity.this, ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "album");
                    intent.putExtra("imageId", id);
                    startActivity(intent);
                    finish();
                }
            }
        }, SearchListAdapter.ItemType.ALBUMS, searchBinding.albumsHeading, searchBinding.searchListAlbumsContainer);
//        searchListAlbumAdapter.setHasStableIds(true);

        searchBinding.searchListAlbumsContainer.setLayoutManager(new GridLayoutManager(this, 2));
        searchBinding.searchListAlbumsContainer.setNestedScrollingEnabled(false);
        searchBinding.searchListAlbumsContainer.setHasFixedSize(true);
        searchBinding.searchListAlbumsContainer.setItemViewCacheSize(20);
        searchBinding.searchListAlbumsContainer.setAdapter(searchListAlbumAdapter);

        searchListArtistAdapter = new SearchListAdapter(this, artists, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isBrowsable()) {
                    Intent intent = new Intent(SearchActivity.this, ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", "artist");
                    startActivity(intent);
                    finish();
                }
            }
        }, SearchListAdapter.ItemType.SONGS, searchBinding.artistsHeading, searchBinding.searchListArtistsContainer);
//        searchListArtistAdapter.setHasStableIds(true);
        searchBinding.searchListArtistsContainer.setLayoutManager(new LinearLayoutManager(this));
        searchBinding.searchListArtistsContainer.setNestedScrollingEnabled(false);
//        searchBinding.searchListArtistsContainer.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        searchBinding.searchListArtistsContainer.setHasFixedSize(true);
        searchBinding.searchListArtistsContainer.setItemViewCacheSize(20);
        searchBinding.searchListArtistsContainer.setAdapter(searchListArtistAdapter);


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
//                searchListSongAdapter.getFilter().filter(newText);
//                searchListAlbumAdapter.getFilter().filter(newText);
//                searchListArtistAdapter.getFilter().filter(newText);
                final String query = newText.toLowerCase();
                if(query.isEmpty())
                {
                    searchBinding.searchListSongsContainer.setVisibility(View.GONE);
                    searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
                    searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
                    searchBinding.songsHeading.setVisibility(View.GONE);
                    searchBinding.albumsHeading.setVisibility(View.GONE);
                    searchBinding.artistsHeading.setVisibility(View.GONE);
                    return false;
                }
                else {

                }
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                        for (MediaBrowserCompat.MediaItem model : songs) {
                            final String text = model.getDescription().getTitle().toString().toLowerCase();
                            if (text.contains(query)) {
                                filteredModelList.add(model);
                            }

                            handler.post(() -> {
                                if(filteredModelList.isEmpty()){
                                    searchBinding.searchListSongsContainer.setVisibility(View.GONE);
                                    searchBinding.songsHeading.setVisibility(View.GONE);
                                }
                                else {
                                    searchBinding.searchListSongsContainer.setVisibility(View.VISIBLE);
                                    searchBinding.songsHeading.setVisibility(View.VISIBLE);
                                }
                                searchListSongAdapter.animateTo(filteredModelList);
                                searchBinding.searchListSongsContainer.scrollToPosition(0);
                            });
                        }
                    }
                });
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                        for (MediaBrowserCompat.MediaItem model : albums) {
                            final String text = model.getDescription().getTitle().toString().toLowerCase();
                            if (text.contains(query)) {
                                filteredModelList.add(model);
                            }

                            handler.post(() -> {
                                if(filteredModelList.isEmpty()){
                                    searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
                                    searchBinding.albumsHeading.setVisibility(View.GONE);
                                }
                                else {
                                    searchBinding.searchListAlbumsContainer.setVisibility(View.VISIBLE);
                                    searchBinding.albumsHeading.setVisibility(View.VISIBLE);
                                }
                                searchListAlbumAdapter.animateTo(filteredModelList);
                                searchBinding.searchListAlbumsContainer.scrollToPosition(0);
                            });
                        }
                    }
                });
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                        for (MediaBrowserCompat.MediaItem model : artists) {
                            final String text = model.getDescription().getTitle().toString().toLowerCase();
                            if (text.contains(query)) {
                                filteredModelList.add(model);
                            }

                            handler.post(() -> {
                                if(filteredModelList.isEmpty()){
                                    searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
                                    searchBinding.artistsHeading.setVisibility(View.GONE);
                                }
                                else {
                                    searchBinding.searchListArtistsContainer.setVisibility(View.VISIBLE);
                                    searchBinding.artistsHeading.setVisibility(View.VISIBLE);
                                }
                                searchListArtistAdapter.animateTo(filteredModelList);
                                searchBinding.searchListArtistsContainer.scrollToPosition(0);
                            });
                        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaBrowser.disconnect();
    }

    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            viewModel.refreshSearchData(SearchActivity.this, mediaBrowser);
            try {
                mediaController = new MediaControllerCompat(SearchActivity.this, mediaBrowser.getSessionToken());
                viewModel.allSearchData.observe(SearchActivity.this, new Observer<List<List<MediaBrowserCompat.MediaItem>>>() {
                    @Override
                    public void onChanged(List<List<MediaBrowserCompat.MediaItem>> lists) {

                        songs.addAll(lists.get(0));
                        searchListSongAdapter.setModels(songs);
                        albums.addAll(lists.get(1));
                        searchListAlbumAdapter.setModels(albums);
                        artists.addAll(lists.get(2));
                        searchListArtistAdapter.setModels(artists);
                        searchBinding.searchListSongsContainer.setVisibility(View.GONE);
                        searchBinding.searchListAlbumsContainer.setVisibility(View.GONE);
                        searchBinding.searchListArtistsContainer.setVisibility(View.GONE);
                        searchBinding.songsHeading.setVisibility(View.GONE);
                        searchBinding.albumsHeading.setVisibility(View.GONE);
                        searchBinding.artistsHeading.setVisibility(View.GONE);
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

    };
}
