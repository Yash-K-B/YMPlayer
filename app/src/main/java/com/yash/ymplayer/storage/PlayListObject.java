package com.yash.ymplayer.storage;

import androidx.room.ColumnInfo;

public class PlayListObject{
    @ColumnInfo(name = "playlist")
    String playlist;
//    @ColumnInfo(name = "count(playlist)")
//    Integer count;

    public String getPlaylist() {
        return playlist;
    }

//    public Integer getCount() {
//        return count;
//    }
}
