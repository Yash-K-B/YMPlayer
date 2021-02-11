package com.yash.ymplayer.util;

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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemAlbumBinding;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.ymplayer.databinding.ItemSearchSongBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.SongViewHolder>{
    private static final String TAG = "SearchListAdapter";

    List<MediaBrowserCompat.MediaItem> songs;
    private OnItemClickListener listener;
    private Context context;
    ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    int listPos;
    int type;
    View headView;
    View recyclerView;

    public SearchListAdapter(Context context, OnItemClickListener listener, int type, View headView, View recyclerview) {
        this.songs = new ArrayList<>();
        this.listener = listener;
        this.context = context;
        this.type = type;
        this.headView = headView;
        this.executor = Executors.newFixedThreadPool(2);
        this.recyclerView = recyclerview;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (type == ItemType.SONGS || type == ItemType.ARTISTS) {
            ItemSearchSongBinding itemSearchSongBinding = ItemSearchSongBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ItemViewHolder(itemSearchSongBinding);
        } else {
            ItemAlbumBinding itemAlbumBinding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AlbumViewHolder(itemAlbumBinding);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        if (type == ItemType.SONGS)
            ((ItemViewHolder) holder).bindSongs(songs.get(holder.getAdapterPosition()), listener);
        else if (type == ItemType.ALBUMS)
            ((AlbumViewHolder) holder).bindAlbums(songs.get(holder.getAdapterPosition()), listener);
        else if (type == ItemType.ARTISTS)
            ((ItemViewHolder) holder).bindArtists(songs.get(holder.getAdapterPosition()), listener);


    }


    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void setModels(List<MediaBrowserCompat.MediaItem> models) {
        songs = new ArrayList<>(models);
    }


    static class SongViewHolder extends RecyclerView.ViewHolder {
        public SongViewHolder(View view) {
            super(view);
        }

    }

    class PlayingQueueViewHolder extends SongViewHolder {
        ItemPlayingQueueBinding binding;

        public PlayingQueueViewHolder(ItemPlayingQueueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindQueueItem(MediaBrowserCompat.MediaItem song, com.yash.ymplayer.util.SongListAdapter.OnItemClickListener listener) {
            binding.trackName.setText(song.getDescription().getTitle());
            binding.trackName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v,song);
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
            long id = Long.parseLong(song.getMediaId().split("[/]",2)[1]);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)).into(new CustomTarget<Drawable>() {
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
                            Drawable drawable = context.getResources().getDrawable(R.drawable.album_art_placeholder,context.getTheme());
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
                    listener.onClick(song, id);
                }

            });

        }
    }

    class ItemViewHolder extends SongViewHolder {
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
    }

    public void updateList(List<MediaBrowserCompat.MediaItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SearchListDiffCallback(this.songs,newList));
        this.songs.clear();
        this.songs.addAll(newList);
        diffResult.dispatchUpdatesTo(this);

    }


    public void animateTo(List<MediaBrowserCompat.MediaItem> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<MediaBrowserCompat.MediaItem> newModels) {
        for (int i = songs.size() - 1; i >= 0; i--) {
            final MediaBrowserCompat.MediaItem model = songs.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }
    private void applyAndAnimateAdditions(List<MediaBrowserCompat.MediaItem> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final MediaBrowserCompat.MediaItem model = newModels.get(i);
            if (!songs.contains(model)) {
                addItem(i, model);
            }
        }
    }
    private void applyAndAnimateMovedItems(List<MediaBrowserCompat.MediaItem> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final MediaBrowserCompat.MediaItem model = newModels.get(toPosition);
            final int fromPosition = songs.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    public void removeItem(int position) {
        songs.remove(position);
        notifyItemRemoved(position);
    }

    public void addItem(int position, MediaBrowserCompat.MediaItem model) {
        songs.add(position, model);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        final MediaBrowserCompat.MediaItem model = songs.remove(fromPosition);
        songs.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
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

    }

    public static class SearchListDiffCallback extends DiffUtil.Callback{
        List<MediaBrowserCompat.MediaItem> oldList,newList;

        public SearchListDiffCallback(List<MediaBrowserCompat.MediaItem> oldList, List<MediaBrowserCompat.MediaItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList!=null?oldList.size():0;
        }

        @Override
        public int getNewListSize() {
            return newList!=null?newList.size():0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMediaId().equals(newList.get(newItemPosition).getMediaId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

}
