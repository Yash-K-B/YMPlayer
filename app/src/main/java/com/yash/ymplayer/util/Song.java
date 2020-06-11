package com.yash.ymplayer.util;

import android.graphics.Color;

public class Song {
    String id,title,subTitle;
    int color;
    int position;

    public Song(String id, String title, String subTitle, int color,int position) {
        this.id = id;
        this.title = title;
        this.subTitle = subTitle;
        this.color = color;
        this.position = position;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public int getColor() {
        return color;
    }

    public int getPosition() {
        return position;
    }
}
