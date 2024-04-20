package com.yash.ymplayer.util;

import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class DiffCallbacks {
    private DiffCallbacks() {}

    public static final DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem> MEDIAITEM_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {

        @Override
        public boolean areItemsTheSame(@NonNull MediaBrowserCompat.MediaItem oldItem, @NonNull MediaBrowserCompat.MediaItem newItem) {
            return oldItem.getMediaId() != null && oldItem.getMediaId().equals(newItem.getMediaId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaBrowserCompat.MediaItem oldItem, @NonNull MediaBrowserCompat.MediaItem newItem) {
            return oldItem.getMediaId() != null
                    && oldItem.getMediaId().equals(newItem.getMediaId())
                    && String.valueOf(oldItem.getDescription().getTitle()).equals(String.valueOf(newItem.getDescription().getTitle()))
                    && String.valueOf(oldItem.getDescription().getSubtitle()).equals(String.valueOf(newItem.getDescription().getSubtitle()));
        }
    };
}
