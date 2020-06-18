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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchViewModel extends ViewModel {
    //albums
    private static final String TAG = "debug";
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> songs = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allAlbums = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allArtists = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allPlaylists = new MutableLiveData<>();
    public MutableLiveData<List<List<MediaBrowserCompat.MediaItem>>> allSearchData = new MutableLiveData<>();
    List<List<MediaBrowserCompat.MediaItem>> searchList = new ArrayList<>(3);;
    int searchListUpdateCalls = 0;

    public Map<String, Drawable> songImages = new HashMap<>();

    public void refresh(Context context, MediaBrowserCompat mediaBrowser) {
        Repository.getRefreshedInstance(context);
        querySongs(mediaBrowser);
        getAllAlbums(mediaBrowser, null);
        getAllArtists(mediaBrowser, null);
    }

    public void refreshSearchData(Context context, MediaBrowserCompat mediaBrowser) {
        searchList = new ArrayList<>(3);
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
            searchList.add(1, allAlbums.getValue());
        } else if (type == UPDATE_TYPE.ARTISTS) {
            searchList.add(2, allArtists.getValue());
        } else if (type == UPDATE_TYPE.ALL_SONGS) {
            searchList.add(0, songs.getValue());
        }
        if (searchListUpdateCalls == 3)
            allSearchData.setValue(searchList);
    }

    interface UPDATE_TYPE {
        int ALL_SONGS = 1;
        int ALBUMS = 2;
        int ARTISTS = 3;
    }
}
