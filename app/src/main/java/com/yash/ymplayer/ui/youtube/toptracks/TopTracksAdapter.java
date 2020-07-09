package com.yash.ymplayer.ui.youtube.toptracks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.util.YoutubeSong;

public class TopTracksAdapter extends PagedListAdapter<YoutubeSong, TopTracksAdapter.TopTracksViewHolder> {
    OnClickListener listener;
    Context context;

    public TopTracksAdapter(Context context, OnClickListener listener) {
        super(DiffCallback);
        this.listener = listener;
        this.context = context;
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
            holder.onBindTracks(song, listener);
    }

    class TopTracksViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        public TopTracksViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void onBindTracks(YoutubeSong song, OnClickListener listener) {
            binding.title.setText(song.getTitle());
            binding.subTitle.setText(song.getChannelTitle());
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
