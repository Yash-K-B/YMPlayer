package com.yash.ymplayer.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.databinding.ItemAlbumBinding;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.ymplayer.databinding.ItemSearchSongBinding;
import com.yash.ymplayer.storage.OfflineMediaProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.SongViewHolder> implements Filterable {
    private static final String TAG = "debug";

    List<MediaBrowserCompat.MediaItem> songs;
    List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
    private OnItemClickListener listener;
    private Context context;
    ExecutorService executor;
    Handler handler = new Handler();
    int listPos;
    int type;
    View headView;
    View recyclerView;

    public SearchListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener, int type, View headView, View recyclerview) {
        this.songs = songs;
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
            ((ItemViewHolder) holder).bindSongs(songs.get(position), listener);
        else if (type == ItemType.ALBUMS)
            ((AlbumViewHolder) holder).bindAlbums(songs.get(position), listener);
        else if (type == ItemType.ARTISTS)
            ((ItemViewHolder) holder).bindArtists(songs.get(position), listener);


    }

    @Override
    public int getItemViewType(int position) {
        //  listPos = getListPosition(position);
        return listPos;
    }

    @Override
    public int getItemCount() {
        return songs.size();
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
                    listener.onClick(song);
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
            long id = Long.parseLong(song.getDescription().getExtras().getString(OfflineMediaProvider.METADATA_KEY_ALBUM_ID));
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
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//
//                    handler.post(() -> {
            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });
//                    });
//
//                }
//            });

        }

        void bindArtists(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    handler.post(() -> {
            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });
//                    });
//                }
//            });

        }
    }

    public void refreshList() {
        allSongs.clear();
        allSongs.addAll(songs);
        songs.clear();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    int constraint_length = 0;
    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MediaBrowserCompat.MediaItem> result_list = new ArrayList<>();
            constraint_length = constraint.length();
            if (constraint != null && constraint.length() != 0) {
                for (int i = 0; i < allSongs.size(); i++) {
                    if (allSongs.get(i).getDescription().getTitle().toString().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        result_list.add(allSongs.get(i));
                        Log.d(TAG, "performFiltering: " + allSongs.get(i).getDescription().getTitle());
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = result_list;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            List<MediaBrowserCompat.MediaItem> result_list = new ArrayList<>((Collection<? extends MediaBrowserCompat.MediaItem>) results.values);
            songs.clear();
            if (!result_list.isEmpty()) songs.addAll(result_list);
            if (constraint_length == 0 || songs.isEmpty()) {
                headView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            } else {
                headView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "publishResults: no of result: " + songs.size());
            notifyDataSetChanged();
        }
    };

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

}
