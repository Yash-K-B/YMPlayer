package com.yash.ymplayer.ui.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.util.Song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalViewModel extends ViewModel {

    //albums
    private static final String TAG = "debug";
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> songs = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allAlbums = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allArtists = new MutableLiveData<>();
    public MutableLiveData<List<MediaBrowserCompat.MediaItem>> allPlaylists = new MutableLiveData<>();

    public Map<String, Drawable> songImages = new HashMap<>();

    public void refresh(Context context, MediaBrowserCompat mediaBrowser) {
        Repository.getRefreshedInstance(context);
        querySongs(mediaBrowser);
        getAllAlbums(mediaBrowser, null);
        getAllArtists(mediaBrowser, null);
    }

    public void querySongs(MediaBrowserCompat browser) {
        Log.d(TAG, "querySongs: ViewModel");
        Bundle extra = new Bundle();
        browser.subscribe("ALL_SONGS", extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                Log.d(TAG, "onChildrenLoaded: ViewModel");
                songs.setValue(children);
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
            }
        });
    }

    public String getAlbumParentId(String parentId) {
        if (parentId == null) {
            return "ALBUMS";
        } else return /*"ALBUMS" + "/" +*/ parentId;
    }


    public void getAllArtists(MediaBrowserCompat mediaBrowser, String parentId) {
        Bundle extra = new Bundle();
        mediaBrowser.subscribe(getArtistParentId(parentId), extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                allArtists.setValue(children);
            }
        });
    }

    public String getArtistParentId(String parentId) {
        if (parentId == null) {
            return "ARTISTS";
        } else return /*"ARTISTS" + "/" +*/ parentId;
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
        } else return /*"PLAYLISTS" + "/" +*/ parentId;
    }

}
