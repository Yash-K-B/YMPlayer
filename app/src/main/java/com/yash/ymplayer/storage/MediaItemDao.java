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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replace(MediaItem item);

    @Query("Select * from MEDIAITEM")
    List<MediaItem> getAll();

    @Query("Select * from MediaItem where mediaId = :mediaId")
    MediaItem getMediaItem(String mediaId);

    @Query("select mi.* from MediaItem mi join PlayList pl on mi.playlistId=pl.id where pl.name = :playlist")
    List<MediaItem> getMediaItemsOfPlaylist(String playlist);

    @Query("select mi.* from MediaItem mi join PlayList pl on mi.playlistId=pl.id where pl.id = :playlistId order by id desc")
    List<MediaItem> getMediaItemsOfPlaylist(int playlistId);

    @Query("select mi.* from MediaItem mi join PlayList pl on mi.playlistId=pl.id where pl.name = :playlist order by id desc")
    List<MediaItem> getMediaItemsOfPlaylistDesc(String playlist);

    @Query("select id, name from PLAYLIST")
    List<PlayListObject> getPlaylists();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PlayList playList);

    @Query("Delete from MediaItem where playlistId = (select id from PlayList where name=:playlist)")
    void deleteSongs(String playlist);

    @Query("Delete from MediaItem where playlistId = :playlistId")
    void deleteSongs(Integer playlistId);

    @Query("Delete from PlayList where id = :id")
    void deletePlaylist(Integer id);

    @Query("Select count(mi.id) from MediaItem mi join PlayList pl on mi.playlistId=pl.id where mediaId = :mediaId and pl.name = :playlist")
    long isAddedTo(String mediaId,String playlist);

    @Query("delete from MediaItem where mediaId = :mediaId and playlistId = (select id from PlayList where name=:playlist)")
    void removeFromPlaylist(String mediaId,String playlist);

    @Query("select * from playlist where name=:playlist")
    PlayList findPlaylist(String playlist);

    @Query("update PlayList set name=:newName where id=:id")
    void renamePlayList(Integer id, String newName);
}
