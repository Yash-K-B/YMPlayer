package com.yash.ymplayer.ui.youtube.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.yash.ymplayer.databinding.ItemPlaylistBannerBinding;
import com.yash.youtube_extractor.models.YoutubePlaylist;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    ItemClickListener listener;
    List<YoutubePlaylist> playlists;
    Context context;

    public PlaylistAdapter(Context context, List<YoutubePlaylist> playlists, ItemClickListener listener) {
        this.listener = listener;
        this.playlists = playlists;
        this.context = context;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaylistBannerBinding binding = ItemPlaylistBannerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PlaylistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        holder.bindPlaylist(playlists.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }


    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ItemPlaylistBannerBinding binding;

        public PlaylistViewHolder(@NonNull ItemPlaylistBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindPlaylist(YoutubePlaylist playlist, ItemClickListener listener) {
            binding.playlistTitle.setText(playlist.getTitle());
            binding.playlistSubTitle.setText(playlist.getDescription());
            binding.itemCount.setText(playlist.getVideoCount());
            Glide.with(context).load(playlist.getArtUrlMedium()).transition(DrawableTransitionOptions.withCrossFade()).into(binding.playlistArt);
            itemView.setOnClickListener(v -> listener.onClick(binding, playlist));
        }
    }

    public interface ItemClickListener {
        void onClick(ItemPlaylistBannerBinding view, YoutubePlaylist playlist);
    }
}
