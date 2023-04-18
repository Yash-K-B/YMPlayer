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
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.ui.youtube.constants.Constants;
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
        Pager<String, YoutubeSong> pager = new Pager<>(pagingConfig, () -> new YoutubePageKeyedDataSource(application.getApplicationContext(), "PL4fGSI1pDJn40WjZ6utkIuj2rNg-7iGsq"));
        return KotlinConverterUtil.Companion.toFlowable(pager.getFlow());
    }

    public LiveData<PagingData<YoutubeSong>> getPlaylistTracks(String playlistId) {
        PagingConfig pagingConfig = new PagingConfig(30, 3, true);
        Pager<String, YoutubeSong> pager = new Pager<>(pagingConfig, () -> new YoutubePageKeyedDataSource(application.getApplicationContext(), playlistId));
        return KotlinConverterUtil.Companion.toFlowable(pager.getFlow());
    }

    public LiveData<List<PopularPlaylist>> getPopularPlaylist() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPopularPlaylist(new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                popularPlaylists.setValue(playlists);
            }

            @Override
            public void onError() {
                popularPlaylists.setValue(null);
            }
        });
        return popularPlaylists;
    }


    public void refreshPopularHit() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPopularPlaylist(new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                popularPlaylists.setValue(playlists);
            }

            @Override
            public void onError() {
                popularPlaylists.setValue(null);
            }
        });
    }


    public LiveData<List<PopularPlaylist>> getAllTimeHitPlaylist() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getAllTimeHitPlaylist(new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                allTimeHitPlaylists.setValue(playlists);
            }

            @Override
            public void onError() {
                allTimeHitPlaylists.setValue(null);
            }
        });
        return allTimeHitPlaylists;
    }

    public void refreshAllTimeHit() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getAllTimeHitPlaylist(new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                allTimeHitPlaylists.setValue(playlists);
            }

            @Override
            public void onError() {
                allTimeHitPlaylists.setValue(null);
            }
        });
    }


    public LiveData<List<PopularPlaylist>> get90sMagic() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPlaylistsDetails(Constants.magic90sPlaylists, new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                magic90s.setValue(playlists);
            }

            @Override
            public void onError() {
                magic90s.setValue(null);
            }
        });
        return magic90s;
    }

    public void refresh90sMagic() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPlaylistsDetails(Constants.magic90sPlaylists, new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                magic90s.setValue(playlists);
            }

            @Override
            public void onError() {
                magic90s.setValue(null);
            }
        });
    }

    public LiveData<List<PopularPlaylist>> getDiscoverNewMusic() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPlaylistsDetails(Constants.discoverNewPlaylists, new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                discoverNewMusic.setValue(playlists);
            }

            @Override
            public void onError() {
                discoverNewMusic.setValue(null);
            }
        });
        return discoverNewMusic;
    }

    public void refreshDiscoverNewMusic() {
        OnlineYoutubeRepository.getInstance(application.getApplicationContext()).getPlaylistsDetails(Constants.discoverNewPlaylists, new OnlineYoutubeRepository.PlaylistsLoadedCallback() {
            @Override
            public void onLoaded(List<PopularPlaylist> playlists) {
                discoverNewMusic.setValue(playlists);
            }

            @Override
            public void onError() {
                discoverNewMusic.setValue(null);
            }
        });
    }


}
