package com.yash.ymplayer.ui.youtube.toptracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.util.YoutubeSong;

public class TopTracksDataSource extends PageKeyedDataSource<String, YoutubeSong> {
    private static final String TAG = "TopTracksDataSource";
    Context context;
    String key1 = "", key2 = "", key3 = "";

    TopTracksDataSource(Context context) {
        this.context = context;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull LoadInitialCallback<String, YoutubeSong> callback) {

        OnlineYoutubeRepository.getInstance(context).topTracks("-1", (tracks, prevToken, nextToken) -> {
            if (!key1.equals("-1")) {
                key1 = "-1";
                callback.onResult(tracks, prevToken, nextToken);
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, YoutubeSong> callback) {

        OnlineYoutubeRepository.getInstance(context).topTracks(params.key, (tracks, prevToken, nextToken) -> {
            if (!key2.equals(params.key)) {
                key2 = params.key;
                callback.onResult(tracks, prevToken);
            }
        });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, YoutubeSong> callback) {

        OnlineYoutubeRepository.getInstance(context).topTracks(params.key, (tracks, prevToken, nextToken) -> {
            if (!key3.equals(params.key)) {
                key3 = params.key;
                callback.onResult(tracks, nextToken);
            }
        });
    }


}
