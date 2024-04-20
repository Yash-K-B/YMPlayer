package com.yash.ymplayer.util;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemAlbumBinding;
import com.yash.ymplayer.interfaces.AlbumOrArtistContextMenuListener;

import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder> {
    private static final String TAG = "AlbumListAdapter";
    private final AsyncListDiffer<MediaBrowserCompat.MediaItem> asyncListDiffer = new AsyncListDiffer<>(this, DiffCallbacks.MEDIAITEM_DIFF_CALLBACK);
    private final OnItemClickListener listener;
    private final Context context;
    AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener;


    public AlbumsAdapter(Context context, OnItemClickListener listener, AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener) {
        this.listener = listener;
        this.context = context;
        this.albumOrArtistContextMenuListener = albumOrArtistContextMenuListener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlbumBinding itemAlbumBinding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AlbumViewHolder(itemAlbumBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem mediaItem = asyncListDiffer.getCurrentList().get(position);
        holder.bindAlbums(mediaItem, listener);
    }

    @Override
    public int getItemCount() {
        return asyncListDiffer.getCurrentList().size();
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumBinding binding;

        public AlbumViewHolder(ItemAlbumBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void bindAlbums(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

            Glide.with(context).load(song.getDescription().getIconUri()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    binding.albumArt.setImageDrawable(resource);
                    Palette.from(((BitmapDrawable) resource).getBitmap())
                            .generate(palette -> {
                                Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                if (textSwatch != null) {
                                    binding.albumItem.setBackgroundColor(textSwatch.getRgb());
                                    binding.albumName.setTextColor(textSwatch.getTitleTextColor());
                                    binding.albumSubText.setTextColor(textSwatch.getBodyTextColor());
                                    binding.albumMore.setColorFilter(textSwatch.getBodyTextColor());
                                }
                            });
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    Drawable drawable = context.getDrawable(R.drawable.album_art_placeholder);
                    binding.albumArt.setImageDrawable(drawable);
                    Palette.from(((BitmapDrawable) drawable).getBitmap())
                            .generate(palette -> {
                                Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                if (textSwatch != null) {
                                    binding.albumItem.setBackgroundColor(textSwatch.getRgb());
                                    binding.albumName.setTextColor(textSwatch.getTitleTextColor());
                                    binding.albumSubText.setTextColor(textSwatch.getBodyTextColor());
                                    binding.albumMore.setColorFilter(textSwatch.getBodyTextColor());
                                }
                            });
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
            binding.albumName.setText(song.getDescription().getTitle());
            binding.albumSubText.setText(song.getDescription().getSubtitle());
            PopupMenu menu = new PopupMenu(context, binding.albumMore);
            menu.inflate(R.menu.albums_context_menu);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.play:
                        albumOrArtistContextMenuListener.play(song, AlbumOrArtistContextMenuListener.ITEM_TYPE.ALBUMS);
                        return true;
                    case R.id.queue_next:
                        albumOrArtistContextMenuListener.queueNext(song, AlbumOrArtistContextMenuListener.ITEM_TYPE.ALBUMS);
                        return true;
                    case R.id.add_to_playlist:
                        albumOrArtistContextMenuListener.addToPlaylist(song, AlbumOrArtistContextMenuListener.ITEM_TYPE.ALBUMS);
                        return true;
                    default:
                        return false;
                }
            });
            binding.albumMore.setOnClickListener(v -> menu.show());
            itemView.setOnClickListener(v -> listener.onClick(song));

        }
    }

    public void refreshList(List<MediaBrowserCompat.MediaItem> newList) {
        asyncListDiffer.submitList(newList);
    }


    public interface OnItemClickListener {
        void onClick(MediaBrowserCompat.MediaItem song);
    }


}
