package com.yash.ymplayer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.SimpleExoPlayer;

public interface PlayerHelper {
    /**
     *  Retrieve and create Playing Queue and set it to the MediaSession
     */
    void setOnlinePlayingQueue(String uri, Bundle extras);

    /**
     *
     * @param mediaId The mediaId of the stored media
     * @return The position in the Queue
     */
    int getPositionInQueue(@NonNull String mediaId);

    /**
     *
     * @param mediaId The mediaId of the stored media
     */
    void resolveQueuePosition(String mediaId);

    /**
     *
     * @param mediaId The mediaId of the stored media
     * @param extras
     */
    void setPlayingQueue(String mediaId, Bundle extras);

    /**
     *
     * @param videoId the id of the online media
     * @param pos the position in the list where to insert
     */
    void addHttpSourceToMediaSources(String videoId, int pos);

    /**
     *
     * @param mediaId The mediaId of the stored media
     * @param pos the position where to add
     */
    void addToMediaSources(@Nullable String mediaId, int pos);

    /**
     *
     * @param player The exoplayer instance
     */
    void preparePlayer(SimpleExoPlayer player);

    /**
     *
     * @param player exoplayer instance
     * @param seekPosition duration to seek in player
     */
    void preparePlayer(SimpleExoPlayer player, long seekPosition);

    /**
     *
     * @param player The exoplayer instance
     * @return exoplayer instance
     */
    SimpleExoPlayer getSimpleExoPlayer(SimpleExoPlayer player);

    /**
     *
     * @param playWhenReady boolean value to play or pause
     */
    void setPlayWhenReady(boolean playWhenReady);
}
