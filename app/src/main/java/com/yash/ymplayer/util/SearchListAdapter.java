package com.yash.ymplayer.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemAlbumBinding;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.ymplayer.databinding.ItemSearchHeaderBinding;
import com.yash.ymplayer.databinding.ItemSearchSongBinding;
import com.yash.ymplayer.interfaces.Keys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.SongViewHolder> {
    private static final String TAG = "SearchListAdapter";

    private final AsyncListDiffer<MediaBrowserCompat.MediaItem> asyncListDiffer = new AsyncListDiffer<>(this, DiffCallbacks.MEDIAITEM_DIFF_CALLBACK);
    private final OnItemClickListener listener;
    private final Context context;
    ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    int listPos;

    public SearchListAdapter(Context context, OnItemClickListener listener) {
        this.listener = listener;
        this.context = context;
        this.executor = Executors.newFixedThreadPool(2);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LogHelper.d(TAG, "onCreateViewHolder: " + viewType);
        if (viewType == ItemType.SONGS || viewType == ItemType.ARTISTS) {
            ItemSearchSongBinding itemSearchSongBinding = ItemSearchSongBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            setLinearLayout(parent, itemSearchSongBinding.getRoot());
            return new ItemViewHolder(itemSearchSongBinding);
        } else if (viewType == ItemType.HEADING) {
            ItemSearchHeaderBinding itemSearchHeaderBinding = ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            setLinearLayout(parent, itemSearchHeaderBinding.getRoot());
            return new HeadingViewHolder(itemSearchHeaderBinding);
        } else {
            ItemAlbumBinding itemAlbumBinding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AlbumViewHolder(itemAlbumBinding);
        }
    }


    private void setLinearLayout(ViewGroup parent, View view) {
        StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        lp.setFullSpan(true);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        int type = getItemViewType(position);
        LogHelper.d(TAG, "onBindViewHolder: " + type);
        MediaBrowserCompat.MediaItem mediaItem = asyncListDiffer.getCurrentList().get(holder.getAbsoluteAdapterPosition());
        if (type == ItemType.SONGS)
            ((ItemViewHolder) holder).bindSongs(mediaItem, listener);
        else if (type == ItemType.ALBUMS)
            ((AlbumViewHolder) holder).bindAlbums(mediaItem, listener);
        else if (type == ItemType.ARTISTS)
            ((ItemViewHolder) holder).bindArtists(mediaItem, listener);
        else if (type == ItemType.HEADING)
            ((HeadingViewHolder) holder).bindHeading(mediaItem, listener);

    }


    @Override
    public int getItemCount() {
        LogHelper.d(TAG, "getItemCount: " + asyncListDiffer.getCurrentList().size());
        return asyncListDiffer.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        int anInt = Objects.requireNonNull(asyncListDiffer.getCurrentList().get(position).getDescription().getExtras()).getInt(Keys.EXTRA_TYPE);
        LogHelper.d(TAG, "getItemViewType: " + anInt);
        return anInt;
    }


    static class SongViewHolder extends RecyclerView.ViewHolder {
        public SongViewHolder(View view) {
            super(view);
        }

    }

    static class PlayingQueueViewHolder extends SongViewHolder {
        ItemPlayingQueueBinding binding;

        public PlayingQueueViewHolder(ItemPlayingQueueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindQueueItem(MediaBrowserCompat.MediaItem song, CategoryAdapter.OnItemClickListener listener) {
            binding.trackName.setText(song.getDescription().getTitle());
            binding.trackName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v, song);
                }
            });
        }
    }

    class AlbumViewHolder extends SongViewHolder {
        ItemAlbumBinding binding;

        public AlbumViewHolder(ItemAlbumBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindAlbums(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            long id = Long.parseLong(song.getMediaId().split("[/]", 2)[1]);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            handler.post(() -> binding.albumArt.setImageDrawable(resource));
                            Palette.from(((BitmapDrawable) resource).getBitmap())
                                    .generate(new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(Palette palette) {
                                            Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                            if (textSwatch != null) {
                                                handler.post(() -> {
                                                    binding.albumItem.setBackgroundColor(textSwatch.getRgb());
                                                    binding.albumName.setTextColor(textSwatch.getTitleTextColor());
                                                    binding.albumSubText.setTextColor(textSwatch.getBodyTextColor());
                                                });

                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            @SuppressLint("UseCompatLoadingForDrawables")
                            Drawable drawable = context.getResources().getDrawable(R.drawable.album_art_placeholder, context.getTheme());
                            handler.post(() -> binding.albumArt.setImageDrawable(drawable));
                            Palette.from(((BitmapDrawable) drawable).getBitmap())
                                    .generate(new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(Palette palette) {
                                            Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                            if (textSwatch != null) {
                                                handler.post(() -> {
                                                    binding.albumItem.setBackgroundColor(textSwatch.getRgb());
                                                    binding.albumName.setTextColor(textSwatch.getTitleTextColor());
                                                    binding.albumSubText.setTextColor(textSwatch.getBodyTextColor());
                                                });

                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
                }
            });
            binding.albumName.setText(song.getDescription().getTitle());
            binding.albumSubText.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }

            });

        }
    }

    static class ItemViewHolder extends SongViewHolder {
        ItemSearchSongBinding binding;

        public ItemViewHolder(ItemSearchSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindSongs(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });

        }

        void bindArtists(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });

        }

        public void bindHeading(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            binding.title.setText(song.getDescription().getTitle());
        }
    }


    static class HeadingViewHolder extends SongViewHolder {
        ItemSearchHeaderBinding binding;

        public HeadingViewHolder(ItemSearchHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindHeading(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            binding.searchHeading.setText(song.getDescription().getTitle());
        }
    }

    public void updateList(List<MediaBrowserCompat.MediaItem> newList) {
        asyncListDiffer.submitList(newList);
    }

    public interface OnItemClickListener {
        default void onClick(MediaBrowserCompat.MediaItem song) {

        }

        default void onClick(MediaBrowserCompat.MediaItem song, long id) {

        }
    }

    public interface ItemType {
        int SONGS = 0;
        int ALBUMS = 1;
        int ARTISTS = 2;
        int HEADING = 3;

    }

}
