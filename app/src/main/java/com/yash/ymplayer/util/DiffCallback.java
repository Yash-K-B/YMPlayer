package com.yash.ymplayer.util;

import android.support.v4.media.MediaBrowserCompat;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

public class DiffCallback extends DiffUtil.Callback {
    List<MediaBrowserCompat.MediaItem> oldList, newList;

    public DiffCallback(List<MediaBrowserCompat.MediaItem> oldList, List<MediaBrowserCompat.MediaItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return Objects.equals(oldList.get(oldItemPosition).getMediaId(), newList.get(newItemPosition).getMediaId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}