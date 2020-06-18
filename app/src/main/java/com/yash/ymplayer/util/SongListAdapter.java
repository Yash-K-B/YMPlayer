package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.ymplayer.databinding.ItemPlaylistBinding;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.main.SongContextMenuListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongViewHolder> implements Filterable {
    private static final String TAG = "debug";

    List<MediaBrowserCompat.MediaItem> songs;
    List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
    private OnItemClickListener listener;
    private int mode;
    private Context context;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    Handler handler = new Handler();
    SongContextMenuListener songContextMenuListener;

    public SongListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener, SongContextMenuListener songContextMenuListener, int mode) {
        this.songs = songs;
        this.listener = listener;
        this.mode = mode;
        this.context = context;
        this.songContextMenuListener = songContextMenuListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mode == 0 || mode == 1) {
            ItemMusicBinding itemMusicBinding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ItemViewHolder(itemMusicBinding);
        } else if (mode == 2) {
            ItemPlaylistBinding itemPlaylistBinding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PlaylistViewHolder(itemPlaylistBinding);
        } else {
            ItemPlayingQueueBinding itemPlayingQueueBinding = ItemPlayingQueueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PlayingQueueViewHolder(itemPlayingQueueBinding);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        if (mode == 0)
            ((ItemViewHolder) holder).bindSongs(songs.get(position), listener);
        else if (mode == 1)
            ((ItemViewHolder) holder).bindArtists(songs.get(position), listener,songContextMenuListener);
        else if (mode == 2)
            ((PlaylistViewHolder) holder).bindPlaylist(songs.get(position), listener);
        else
            ((PlayingQueueViewHolder) holder).bindQueueItem(songs.get(position), listener);


    }

    @Override
    public int getItemViewType(int position) {
        return mode;
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

        void bindQueueItem(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            binding.trackName.setText(song.getDescription().getTitle());
            binding.trackName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });
        }
    }

    class PlaylistViewHolder extends SongViewHolder {
        ItemPlaylistBinding binding;

        public PlaylistViewHolder(ItemPlaylistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindPlaylist(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {
            if ((Keys.PLAYLISTS.FAVOURITES).equals(song.getDescription().getTitle() + ""))
                binding.iconPlaylist.setImageResource(R.drawable.icon_favourite);
            binding.playlistTitle.setText(song.getDescription().getTitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });
        }
    }

    class ItemViewHolder extends SongViewHolder {
        ItemMusicBinding binding;

        public ItemViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindSongs(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            String[] parts = song.getDescription().getMediaId().split("[/|]");
            long id = Long.parseLong(parts[parts.length - 1]);
            Log.d(TAG, "run: id: " + id);
            retriever.setDataSource(context, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id));
            Glide.with(context).load(retriever.getEmbeddedPicture()).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    handler.post(() -> binding.art.setImageDrawable(resource));
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    Log.d(TAG, "onLoadCleared: ");
                }
            });
//                }
//            });

            PopupMenu menu = new PopupMenu(context, binding.more);
            menu.getMenuInflater().inflate(R.menu.song_context_menu, menu.getMenu());
            //binding.more.;
            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(song);
                }
            });

        }

        void bindArtists(MediaBrowserCompat.MediaItem song, OnItemClickListener listener,SongContextMenuListener songContextMenuListener) {
           executor.execute(new Runnable() {
               @Override
               public void run() {
                   PopupMenu menu = new PopupMenu(context, binding.more);
                   menu.getMenuInflater().inflate(R.menu.artists_context_menu, menu.getMenu());
                   menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                       @Override
                       public boolean onMenuItemClick(MenuItem item) {
                           switch (item.getItemId()) {
                               case R.id.play:
                                   return true;
                               case R.id.queue_next:
                                   songContextMenuListener.queueNext(song);
                                   return true;
                               case R.id.add_to_playlist:
                                   AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                   builder.setTitle("Choose Playlist");
                                   List<MediaBrowserCompat.MediaItem> lists = Repository.getInstance(context).getAllPlaylists();
                                   String[] list = new String[lists.size()];
                                   for (int i = 0; i < lists.size(); i++) {
                                       list[i] = lists.get(i).getDescription().getTitle().toString();
                                   }
                                   builder.setItems(list, (dialog, which) -> songContextMenuListener.addToPlaylist(song, list[which]));
                                   AlertDialog dialog = builder.create();
                                   dialog.show();
                                   return true;
                               default:
                                   return false;
                           }
                       }
                   });
                   handler.post(()->binding.more.setOnClickListener(v -> menu.show()));
               }
           });

            binding.art.setVisibility(View.GONE);
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

    public void refreshList() {
        allSongs.clear();
        allSongs.addAll(songs);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    Filter filter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MediaBrowserCompat.MediaItem> filteredList = new ArrayList<>();
            Log.d(TAG, "performFiltering: allsongs size:" + songs.size());
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(allSongs);
                Log.d(TAG, "performFiltering: All list return");
            } else {
                for (MediaBrowserCompat.MediaItem item : allSongs) {
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
            songs.clear();
            songs.addAll((Collection<? extends MediaBrowserCompat.MediaItem>) results.values);
            notifyDataSetChanged();
        }
    };

    public interface OnItemClickListener {
        void onClick(MediaBrowserCompat.MediaItem song);
    }
}
