package com.yash.ymplayer.storage;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Entity(indices = {@Index(value = {"name"}, unique = true)})
public class PlayList {
    @PrimaryKey(autoGenerate = true)
    private Integer id;
    private String name;

    public PlayList(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}