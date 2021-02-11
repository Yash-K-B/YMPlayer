package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemAlbumBinding;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.ui.main.AlbumOrArtistContextMenuListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder> {
    private static final String TAG = "AlbumListAdapter";
    private List<MediaBrowserCompat.MediaItem> albums;
    List<MediaBrowserCompat.MediaItem> allAlbums = new ArrayList<>();
    private  OnItemClickListener listener;
    private Context context;
    AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener;


    public AlbumListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener, AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener) {
        this.albums = songs;
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
        holder.bindAlbums(albums.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumBinding binding;

        public AlbumViewHolder(ItemAlbumBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void bindAlbums(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

            Glide.with(context).load(song.getDescription().getIconUri()).into(new CustomTarget<Drawable>() {
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Choose Playlist");
                        List<MediaBrowserCompat.MediaItem> lists = Repository.getInstance(context).getAllPlaylists();
                        String[] list = new String[lists.size()];
                        for (int i = 0; i < lists.size(); i++) {
                            list[i] = lists.get(i).getDescription().getTitle() + "";
                        }
                        builder.setItems(list, (dialog, which) -> albumOrArtistContextMenuListener.addToPlaylist(song, list[which], AlbumOrArtistContextMenuListener.ITEM_TYPE.ALBUMS));
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return true;
                    default:
                        return false;
                }
            });
            binding.albumMore.setOnClickListener(v -> menu.show());
            itemView.setOnClickListener(v -> listener.onClick(song));

        }
    }

    public void refreshList() {
        allAlbums.clear();
        allAlbums.addAll(albums);
    }


    public interface OnItemClickListener {
        void onClick(MediaBrowserCompat.MediaItem song);
    }


}
