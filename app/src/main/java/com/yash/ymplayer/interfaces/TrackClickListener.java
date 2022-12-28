package com.yash.ymplayer.interfaces;

import com.yash.ymplayer.util.YoutubeSong;

public interface TrackClickListener {
    void onClick(YoutubeSong song);

    void onPlaySingle(YoutubeSong song);

    void onQueueNext(YoutubeSong song);

    void onQueueLast(YoutubeSong song);

    void addToPlaylist(YoutubeSong song);

    void download(YoutubeSong song, int bitRateInKbps);
}