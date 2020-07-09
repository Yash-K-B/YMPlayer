package com.yash.ymplayer.models;

import java.util.ArrayList;
import java.util.List;

public class PopularPlaylists {

    List<PopularPlaylist> items = new ArrayList<>();

    public List<PopularPlaylist> getItems() {
        return items;
    }

    public void setItems(List<PopularPlaylist> items) {
        this.items = items;
    }
}
