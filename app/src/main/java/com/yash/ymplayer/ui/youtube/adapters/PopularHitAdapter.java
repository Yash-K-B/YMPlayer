package com.yash.ymplayer.ui.youtube.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yash.ymplayer.databinding.ItemPlaylistBannerBinding;
import com.yash.ymplayer.models.PopularPlaylist;

import java.util.List;

public class PopularHitAdapter extends RecyclerView.Adapter<PopularHitAdapter.PopularPlaylistViewHolder> {
    ItemClickListener listener;
    List<PopularPlaylist> playlists;
    Context context;

    public PopularHitAdapter(Context context, List<PopularPlaylist> playlists, ItemClickListener listener) {
        this.listener = listener;
        this.playlists = playlists;
        this.context = context;
    }

    @NonNull
    @Override
    public PopularPlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaylistBannerBinding binding = ItemPlaylistBannerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PopularPlaylistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PopularPlaylistViewHolder holder, int position) {
        holder.bindPlaylist(playlists.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }


    class PopularPlaylistViewHolder extends RecyclerView.ViewHolder {
        ItemPlaylistBannerBinding binding;

        public PopularPlaylistViewHolder(@NonNull ItemPlaylistBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindPlaylist(PopularPlaylist playlist, ItemClickListener listener) {
            binding.playlistTitle.setText(playlist.getSnippet().getLocalized().getTitle());
            binding.playlistSubTitle.setText(playlist.getSnippet().getLocalized().getDescription());
            binding.itemCount.setText(playlist.getContentDetails().getItemCount() + " Tracks");
            Glide.with(context).load(playlist.getSnippet().getThumbnails().getMedium().getUrl()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.playlistArt);
            itemView.setOnClickListener(v -> listener.onClick(binding, playlist));
        }
    }

    public interface ItemClickListener {
        void onClick(ItemPlaylistBannerBinding view, PopularPlaylist playlist);
    }
}
