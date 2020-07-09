package com.yash.ymplayer.ui.youtube.toptracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.yash.ymplayer.util.YoutubeSong;

public class TopTracksDataSourceFactory extends PageKeyedDataSource.Factory<String, YoutubeSong> {
    Context context;

    public TopTracksDataSourceFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public DataSource<String, YoutubeSong> create() {
        return new TopTracksDataSource(context);
    }
}
