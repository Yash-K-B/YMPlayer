package com.yash.ymplayer.storage;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity
public class PlayList {
    @NonNull
    @PrimaryKey()
    String playlist;

    public PlayList(String playlist) {
        this.playlist = playlist;
    }
}
