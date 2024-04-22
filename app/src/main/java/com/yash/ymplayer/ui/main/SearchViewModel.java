package com.yash.ymplayer.ui.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.SearchListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SearchViewModel extends ViewModel {
    private static final String TAG = "SearchViewModel";
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> songs = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allAlbums = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allArtists = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allPlaylists = new MutableLiveData<>();
    public MutableLiveData<List<List<MediaBrowserCompat.MediaItem>>> allSearchData = new MutableLiveData<>();

    private final int SEARCH_CATEGORY = 3;
    private final List<List<MediaBrowserCompat.MediaItem>> searchList = new ArrayList<>(SEARCH_CATEGORY);
    int searchListUpdateCalls = 0;

    public SearchViewModel() {
        for (int i = 0; i < SEARCH_CATEGORY; i++) {
            searchList.add(new ArrayList<>());
        }
    }

    public Map<String, Drawable> songImages = new HashMap<>();

    public void refresh(Context context, MediaBrowserCompat mediaBrowser) {
        querySongs(mediaBrowser);
        getAllAlbums(mediaBrowser, null);
        getAllArtists(mediaBrowser, null);
    }

    public void refreshSearchData(Context context, MediaBrowserCompat mediaBrowser) {
        searchListUpdateCalls = 0;
        refresh(context, mediaBrowser);
    }

    public void querySongs(MediaBrowserCompat browser) {
        Log.d(TAG, "querySongs: ViewModel");
        Bundle extra = new Bundle();
        browser.subscribe("ALL_SONGS", extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                Log.d(TAG, "onChildrenLoaded: ViewModel");
                songs.setValue(children);
                updateSearchData(UPDATE_TYPE.ALL_SONGS);
            }
        });
        Log.d(TAG, "querySongs: Subscribed");
    }


    public void getAllAlbums(MediaBrowserCompat mediaBrowser, String parentId) {
        Bundle extra = new Bundle();
        mediaBrowser.subscribe(getAlbumParentId(parentId), extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                allAlbums.setValue(children);
                updateSearchData(UPDATE_TYPE.ALBUMS);
            }
        });
    }

    public String getAlbumParentId(String parentId) {
        if (parentId == null) {
            return "ALBUMS";
        } else return "ALBUMS" + "/" + parentId;
    }


    public void getAllArtists(MediaBrowserCompat mediaBrowser, String parentId) {
        Bundle extra = new Bundle();
        mediaBrowser.subscribe(getArtistParentId(parentId), extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                allArtists.setValue(children);
                updateSearchData(UPDATE_TYPE.ARTISTS);
            }
        });
    }

    public String getArtistParentId(String parentId) {
        if (parentId == null) {
            return "ARTISTS";
        } else return "ARTISTS" + "/" + parentId;
    }

    public void getAllPlaylists(MediaBrowserCompat mediaBrowser, String parentId) {
        Bundle extra = new Bundle();
        extra.putString("type", "artists");
        mediaBrowser.subscribe(getPlaylistParentId(parentId), extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                allPlaylists.setValue(children);
            }
        });
    }

    public String getPlaylistParentId(String parentId) {
        if (parentId == null) {
            return "PLAYLISTS";
        } else return "PLAYLISTS" + "/" + parentId;
    }

    void updateSearchData(int type) {
        searchListUpdateCalls++;
        if (type == UPDATE_TYPE.ALBUMS) {
            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for (MediaBrowserCompat.MediaItem mediaItem : Objects.requireNonNull(allAlbums.getValue())) {
                MediaBrowserCompat.MediaItem item = mapToNew(mediaItem, SearchListAdapter.ItemType.ALBUMS);
                mediaItems.add(item);
            }
            searchList.set(1, mediaItems);
        } else if (type == UPDATE_TYPE.ARTISTS) {
            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for (MediaBrowserCompat.MediaItem mediaItem : Objects.requireNonNull(allArtists.getValue())) {
                MediaBrowserCompat.MediaItem item = mapToNew(mediaItem, SearchListAdapter.ItemType.ARTISTS);
                mediaItems.add(item);
            }
            searchList.set(2, mediaItems);
        } else if (type == UPDATE_TYPE.ALL_SONGS) {
            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for (MediaBrowserCompat.MediaItem mediaItem : Objects.requireNonNull(songs.getValue())) {
                MediaBrowserCompat.MediaItem item = mapToNew(mediaItem, SearchListAdapter.ItemType.SONGS);
                mediaItems.add(item);
            }
            searchList.set(0, mediaItems);
        }
        if (searchListUpdateCalls == 3)
            allSearchData.setValue(searchList);
    }

    private MediaBrowserCompat.MediaItem mapToNew(MediaBrowserCompat.MediaItem mediaItem, int type) {
        MediaDescriptionCompat description = mediaItem.getDescription();
        Bundle extra = new Bundle();
        extra.putInt(Keys.EXTRA_TYPE, type);
        return new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(mediaItem.getMediaId())
                .setTitle(description.getTitle())
                .setSubtitle(description.getSubtitle())
                .setDescription(description.getDescription())
                .setIconUri(description.getIconUri())
                .setMediaUri(description.getMediaUri())
                .setExtras(extra)

                .build(), mediaItem.getFlags());
    }

    interface UPDATE_TYPE {
        int ALL_SONGS = 1;
        int ALBUMS = 2;
        int ARTISTS = 3;
    }
}
