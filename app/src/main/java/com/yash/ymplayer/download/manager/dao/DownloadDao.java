package com.yash.ymplayer.download.manager.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.yash.ymplayer.download.manager.constants.DownloadStatus;
import com.yash.ymplayer.download.manager.models.Download;

import java.util.List;

@Dao
public interface DownloadDao {

    @Insert
    long insert(Download download);

    @Query("select * from Download where id=:id")
    Download find(int id);

    @Query("select count(videoId) from Download")
    int getCount();

    @Query("delete from Download where id=:id")
    int delete(int id);

    @Query("update Download set status=:status where id=:id")
    void updateStatus(int id, String status);

    @Query("update Download set uri=:uri where id=:id")
    void updateUri(int id, String uri);

    @Query("update Download set fileLength=:length where id=:id")
    void updateLength(int id, long length);

    @Query("select * from Download")
    List<Download> findAll();
}
