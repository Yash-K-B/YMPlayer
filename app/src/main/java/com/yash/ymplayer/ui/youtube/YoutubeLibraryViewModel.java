package com.yash.ymplayer.ui.youtube;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.ui.youtube.todayspopular.PopularHit;
import com.yash.ymplayer.ui.youtube.toptracks.TopTracksDataSourceFactory;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public class YoutubeLibraryViewModel extends AndroidViewModel {
    Application application;
    MutableLiveData<List<PopularPlaylist>> popularPlaylists = new MutableLiveData<>();
    MutableLiveData<List<PopularPlaylist>> allTimeHitPlaylists = new MutableLiveData<>();

    public YoutubeLibraryViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

   public LiveData<PagedList<YoutubeSong>> getTopTracks(){
        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(50)
                .setPrefetchDistance(50)
                .build();
        return new LivePagedListBuilder<>(new TopTracksDataSourceFactory(application.getApplicationContext()),config).build();
   }

   public LiveData<List<PopularPlaylist>> getPopularPlaylist(){
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


   public LiveData<List<PopularPlaylist>> getAllTimeHitPlaylist(){
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




}
