package com.yash.ymplayer.storage;

import androidx.room.ColumnInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PlayListObject{
    @ColumnInfo(name = "id")
    private Integer id;
    @ColumnInfo(name = "name")
    private String name;

    public PlayListObject(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public PlayListObject() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
