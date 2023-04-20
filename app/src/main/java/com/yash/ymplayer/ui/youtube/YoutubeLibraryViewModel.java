package com.yash.ymplayer.ui.youtube;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;

import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.ui.youtube.livepage.YoutubePageKeyedDataSource;
import com.yash.ymplayer.util.KotlinConverterUtil;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;


public class YoutubeLibraryViewModel extends AndroidViewModel {
    Application application;
    MutableLiveData<List<PopularPlaylist>> popularPlaylists = new MutableLiveData<>();
    MutableLiveData<List<PopularPlaylist>> allTimeHitPlaylists = new MutableLiveData<>();
    MutableLiveData<List<PopularPlaylist>> magic90s = new MutableLiveData<>();
    MutableLiveData<List<PopularPlaylist>> discoverNewMusic = new MutableLiveData<>();

    public YoutubeLibraryViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<PagingData<YoutubeSong>> getTopTracks() {
        PagingConfig pagingConfig = new PagingConfig(30, 3, true);
        Pager<String, YoutubeSong> pager = new Pager<>(pagingConfig, () -> new YoutubePageKeyedDataSource(application.getApplicationContext(), com.yash.ymplayer.constant.Constants.DEFAULT_PLAYLIST));
        return KotlinConverterUtil.Companion.toLiveData(pager.getFlow());
    }

    public LiveData<PagingData<YoutubeSong>> getPlaylistTracks(String playlistId) {
        PagingConfig pagingConfig = new PagingConfig(30, 3, true);
        Pager<String, YoutubeSong> pager = new Pager<>(pagingConfig, () -> new YoutubePageKeyedDataSource(application.getApplicationContext(), playlistId));
        return KotlinConverterUtil.Companion.toLiveData(pager.getFlow());
    }

    public LiveData<List<PopularPlaylist>> getPopularPlaylist() {
        return popularPlaylists;
    }


    public void refreshPopularHit() {
    }


    public LiveData<List<PopularPlaylist>> getAllTimeHitPlaylist() {
        return allTimeHitPlaylists;
    }

    public void refreshAllTimeHit() {
    }


    public LiveData<List<PopularPlaylist>> get90sMagic() {
        return magic90s;
    }

    public void refresh90sMagic() {
    }

    public LiveData<List<PopularPlaylist>> getDiscoverNewMusic() {
        return discoverNewMusic;
    }

    public void refreshDiscoverNewMusic() {
    }


}
