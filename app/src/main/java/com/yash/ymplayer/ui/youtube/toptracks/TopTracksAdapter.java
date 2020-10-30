package com.yash.ymplayer.ui.youtube.toptracks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yash.ymplayer.DownloadService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.YoutubeSong;

import java.security.Key;

public class TopTracksAdapter extends PagedListAdapter<YoutubeSong, TopTracksAdapter.TopTracksViewHolder> {
    OnClickListener listener;
    MediaControllerCompat mediaController;
    Context context;

    public TopTracksAdapter(Context context, OnClickListener listener, MediaControllerCompat mediaController) {
        super(DiffCallback);
        this.listener = listener;
        this.context = context;
        this.mediaController = mediaController;
    }

    @NonNull
    @Override
    public TopTracksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicBinding binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new TopTracksViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopTracksViewHolder holder, int position) {
        YoutubeSong song = getItem(position);
        if (song != null)
            holder.onBindTracks(song, listener,mediaController);
    }

    class TopTracksViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        public TopTracksViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void onBindTracks(YoutubeSong song, OnClickListener listener,MediaControllerCompat  mediaController) {
            PopupMenu menu = new PopupMenu(context,binding.more);
            menu.inflate(R.menu.youtube_song_menu);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()){
                    case R.id.play_single:
                        return true;
                    case R.id.download:
                        Intent downloadIntent = new Intent(context,DownloadService.class);
                        downloadIntent.putExtra(Keys.VIDEO_ID,song.getVideoId());
                        context.startService(downloadIntent);
                        return true;
                    default: return false;
                }
            });
            binding.title.setText(song.getTitle());
            binding.subTitle.setText(song.getChannelTitle());
            binding.more.setOnClickListener(v -> menu.show());
            Glide.with(context).load(song.getArt_url_small()).into(binding.art);
            itemView.setOnClickListener(v -> listener.onClick(song));
        }
    }

    interface OnClickListener {
        void onClick(YoutubeSong song);
    }


    private static final DiffUtil.ItemCallback<YoutubeSong> DiffCallback = new DiffUtil.ItemCallback<YoutubeSong>() {
        @Override
        public boolean areItemsTheSame(@NonNull YoutubeSong oldItem, @NonNull YoutubeSong newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull YoutubeSong oldItem, @NonNull YoutubeSong newItem) {
            return oldItem.getVideoId().equals(newItem.getVideoId());
        }
    };
}
