package com.yash.ymplayer.storage;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PlayList.class,MediaItem.class},version = 3,exportSchema = false)
public abstract class PlaylistMediaProvider extends RoomDatabase {
    public abstract MediaItemDao getMediaItemDao();
}
