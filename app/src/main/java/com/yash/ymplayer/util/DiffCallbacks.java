package com.yash.ymplayer.util;

import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.yash.ymplayer.download.manager.models.Download;

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

    public static final DiffUtil.ItemCallback<Download> DOWNLOAD_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {

        @Override
        public boolean areItemsTheSame(@NonNull Download oldItem, @NonNull Download newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Download oldItem, @NonNull Download newItem) {
            return oldItem.getFileName().equals(newItem.getFileName());
        }
    };;

}
