package com.yash.ymplayer.ui.spotify;

import android.support.v4.media.MediaBrowserCompat;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

class SpotifyViewModel extends ViewModel {
    MutableLiveData<List<MediaBrowserCompat.MediaItem>> albums = new MutableLiveData<>();

    void getSpotifyAlbums(MediaBrowserCompat mediaBrowser){
        //mediaBrowser.subscribe();
    }
}
