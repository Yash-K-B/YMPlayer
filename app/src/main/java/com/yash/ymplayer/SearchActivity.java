package com.yash.ymplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
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
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.ActivitySearchBinding;
import com.yash.ymplayer.databinding.BasePlayerActivityBinding;
import com.yash.ymplayer.ui.main.SearchViewModel;
import com.yash.ymplayer.ui.youtube.search.YoutubeSearch;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.SearchListAdapter;
import com.yash.ymplayer.util.StringUtil;

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

    SearchListAdapter searchResultAdapter;

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
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        this.mediaBrowser = mediaBrowser;
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1);

        searchResultAdapter = new SearchListAdapter(this, new SearchListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song) {
                if (song.isPlayable()) {
                    searchView.clearFocus();
                    mediaController.getTransportControls().playFromMediaId(song.getMediaId(), null);
                } else if (song.isBrowsable()) {
                    Intent intent = new Intent(SearchActivity.this, ListExpandActivity.class);
                    intent.putExtra("parent_id", song.getMediaId());
                    intent.putExtra("type", song.getDescription().getExtras().getInt(Keys.EXTRA_TYPE) == SearchListAdapter.ItemType.ALBUMS ? "album" : "artist");
                    startActivity(intent);
                }
            }
        });
        searchBinding.listRv.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        searchBinding.listRv.setItemViewCacheSize(20);
        searchBinding.listRv.setAdapter(searchResultAdapter);

        searchBinding.noResult.setVisibility(View.VISIBLE);

        searchBinding.onlineSearch.setOnClickListener(onlineSearchClickListener);
    }

    @Override
    protected void onConnected(MediaControllerCompat mediaController) {
        this.mediaController = mediaController;
        viewModel.refreshSearchData(SearchActivity.this, mediaBrowser);
        viewModel.allSearchData.observe(SearchActivity.this, lists -> SearchActivity.this.lists = lists);
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
                LogHelper.d(TAG, "onQueryTextChange: text - %s", newText);
                if (StringUtil.hasText(newText)) {
                    searchBinding.onlineSearch.setVisibility(View.VISIBLE);
                    searchBinding.onlineSearch.setText("\tSearch online \"" + newText + "\"");
                } else {
                    searchBinding.onlineSearch.setVisibility(View.INVISIBLE);
                }

                handler.removeCallbacks(null);
                final String query = newText.toLowerCase();
                if (query.isEmpty()) {
                    searchResultAdapter.updateList(new ArrayList<>());
                    searchBinding.noResult.setVisibility(View.VISIBLE);
                    return true;
                }

                searchBinding.noResult.setVisibility(View.GONE);


                handler.post(new RunnableWithParams(query) {
                    @Override
                    public void run(String text) {
                        LogHelper.d(TAG, "run: Searching with key - %s", text);

                        executorService.execute(new RunnableWithParams(text) {
                            @Override
                            public void run(String text) {
                                final List<MediaBrowserCompat.MediaItem> filteredModelList = new ArrayList<>();
                                for (int i = 0; i < lists.size(); i++) {
                                    List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
                                    for (MediaBrowserCompat.MediaItem model : lists.get(i)) {
                                        final String title = model.getDescription().getTitle().toString().toLowerCase();
                                        if (title.contains(text)) {
                                            items.add(model);
                                        }
                                    }
                                    if (!items.isEmpty()) {
                                        String title = (i == 0) ? "Songs" : ((i == 1) ? "Albums" : "Artists");
                                        Bundle bundle = new Bundle();
                                        bundle.putInt(Keys.EXTRA_TYPE, SearchListAdapter.ItemType.HEADING);
                                        filteredModelList.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                                                .setMediaId("0").setTitle(title).setExtras(bundle).build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                                        filteredModelList.addAll(items);
                                    }

                                }

                                handler.post(() -> {
                                    LogHelper.d(TAG, "run: update filtered list size - %s", filteredModelList.size());
                                    searchBinding.noResult.setVisibility(filteredModelList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
                                    searchResultAdapter.updateList(filteredModelList);

                                    if(filteredModelList.isEmpty()) {
                                        searchBinding.noResult.setVisibility(View.VISIBLE);
                                    }
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

    View.OnClickListener onlineSearchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SearchActivity.this, YoutubeSearch.class);
            intent.putExtra("query", searchView.getQuery().toString());
            startActivity(intent);
            finish();
        }
    };
}
