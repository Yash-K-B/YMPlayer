package com.yash.ymplayer.download.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.yash.ymplayer.download.manager.dao.DownloadDao;

public class DownloadRepository {

    private DownloadDao downloadDao;

    private static DownloadRepository instance;

    public static DownloadRepository getInstance(Context context) {
        if (instance == null)
            instance = new DownloadRepository(context.getApplicationContext());
        return instance;
    }


    private DownloadRepository(Context context) {
        RoomDatabase.Callback databaseCallback = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };
        Downloads downloads = Room.databaseBuilder(context, Downloads.class, "downloads.db").allowMainThreadQueries().fallbackToDestructiveMigration().addCallback(databaseCallback).build();
        downloadDao = downloads.getDownloadDao();
    }

    public DownloadDao getDownloadDao() {
        return downloadDao;
    }
}
