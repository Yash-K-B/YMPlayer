package com.yash.ymplayer.download.manager;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.yash.ymplayer.download.manager.dao.DownloadDao;
import com.yash.ymplayer.download.manager.models.Download;

@Database(entities = {Download.class},version = 1,exportSchema = false)
public abstract class Downloads extends RoomDatabase {
    public abstract DownloadDao getDownloadDao();
}
