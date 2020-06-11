package com.yash.ymplayer.storage;

import android.database.Cursor;
import android.util.Pair;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Dao
public interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(MediaItem item);

    @Update
    void update(MediaItem item);

    @Delete
    void delete(MediaItem item);


    @Query("Select * from MEDIAITEM")
    List<MediaItem> getAll();

    @Query("Select * from MediaItem where mediaId = :mediaId")
    MediaItem getMediaItem(String mediaId);

    @Query("select * from MediaItem where playlist = :playlist")
    List<MediaItem> getMediaItemsOfPlaylist(String playlist);

    @Query("select playlist from PLAYLIST")
    List<PlayListObject> getPlaylists();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PlayList playList);

    @Query("Delete from mediaitem where playlist = :playlist;")
    void deleteSongs(String playlist);

    @Query("Delete from PlayList where playlist = :playlist")
    void deletePlaylist(String playlist);

    @Query("Select count(*) from mediaitem where mediaId = :mediaId and playlist = :playlist")
    long isAddedTo(String mediaId,String playlist);

    @Query("delete from mediaitem where mediaId = :mediaId and playlist = :playlist")
    void removeFromPlaylist(String mediaId,String playlist);
}
