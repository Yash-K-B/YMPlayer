package com.yash.ymplayer.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.yash.ymplayer.storage.OfflineMediaProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder> implements Filterable {
    private static final String TAG = "debug";
    private List<MediaBrowserCompat.MediaItem> albums;
    List<MediaBrowserCompat.MediaItem> allAlbums = new ArrayList<>();
    private OnItemClickListener listener;
    private Context context;


    public AlbumListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener) {
        this.albums = songs;
        this.listener = listener;
        this.context = context;
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
        }

        void bindAlbums(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            long id = Long.parseLong(song.getDescription().getExtras().getString(OfflineMediaProvider.METADATA_KEY_ALBUM_ID));
            Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    binding.albumArt.setImageDrawable(resource);
                    Palette.from(((BitmapDrawable) resource).getBitmap())
                            .generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                    if (textSwatch != null) {
                                        binding.albumItem.setBackgroundColor(textSwatch.getRgb());
                                        binding.albumName.setTextColor(textSwatch.getTitleTextColor());
                                        binding.albumSubText.setTextColor(textSwatch.getBodyTextColor());
                                    }
                                }
                            });
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
            binding.albumName.setText(song.getDescription().getTitle());
            binding.albumSubText.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song,id);
                }
            });
        }
    }

    public void refreshList(){
        allAlbums.clear();
        allAlbums.addAll(albums);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    Filter filter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MediaBrowserCompat.MediaItem> filteredList = new ArrayList<>();
            Log.d(TAG, "performFiltering: allsongs size:" + albums.size());
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(allAlbums);
                Log.d(TAG, "performFiltering: All list return");
            } else {
                for (MediaBrowserCompat.MediaItem item : allAlbums) {
                    if (item.getDescription().getTitle().toString().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Log.d(TAG, "publishResults: new size :");
            albums.clear();
            albums.addAll((Collection<? extends MediaBrowserCompat.MediaItem>) results.values);
            notifyDataSetChanged();
        }
    };

    public interface OnItemClickListener {
        void onClick(MediaBrowserCompat.MediaItem song,long id);
    }
}
