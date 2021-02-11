package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
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
import com.yash.ymplayer.ui.main.AlbumOrArtistContextMenuListener;
import com.yash.ymplayer.ui.main.SongContextMenuListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.yash.ymplayer.ui.main.AlbumOrArtistContextMenuListener.*;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongViewHolder> {
    private static final String TAG = "debug";

    List<MediaBrowserCompat.MediaItem> songs;
    List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
    private OnItemClickListener listener;
    private int mode;
    private Context context;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());
    AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener;

    public SongListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener, AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener, int mode) {
        this.songs = songs;
        this.listener = listener;
        this.mode = mode;
        this.context = context;
        this.albumOrArtistContextMenuListener = albumOrArtistContextMenuListener;
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
            ((ItemViewHolder) holder).bindArtists(songs.get(position), listener, albumOrArtistContextMenuListener);
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
                    listener.onClick(v, song);
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
            else if ("Last Added".equals(song.getDescription().getTitle() + ""))
                binding.iconPlaylist.setImageResource(R.drawable.icon_last_added);
            binding.playlistTitle.setText(song.getDescription().getTitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v, song);
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
                    listener.onClick(v, song);
                }
            });

        }

        void bindArtists(MediaBrowserCompat.MediaItem song, OnItemClickListener listener, AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    PopupMenu menu = new PopupMenu(context, binding.more);
                    menu.inflate(R.menu.artists_context_menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.play:
                                    albumOrArtistContextMenuListener.play(song, ITEM_TYPE.ARTISTS);
                                    return true;
                                case R.id.queue_next:
                                    albumOrArtistContextMenuListener.queueNext(song, ITEM_TYPE.ARTISTS);
                                    return true;
                                case R.id.add_to_playlist:
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Choose Playlist");
                                    List<MediaBrowserCompat.MediaItem> lists = Repository.getInstance(context).getAllPlaylists();
                                    String[] list = new String[lists.size()];
                                    for (int i = 0; i < lists.size(); i++) {
                                        list[i] = lists.get(i).getDescription().getTitle() + "";
                                    }
                                    builder.setItems(list, (dialog, which) -> albumOrArtistContextMenuListener.addToPlaylist(song, list[which], ITEM_TYPE.ARTISTS));
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    handler.post(() -> binding.more.setOnClickListener(v -> {
                        menu.show();
                    }));
                }
            });

            binding.art.setVisibility(View.GONE);
            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(v -> listener.onClick(v, song));
        }
    }

    public void refreshList() {
        allSongs.clear();
        allSongs.addAll(songs);
    }


    public interface OnItemClickListener {
        void onClick(View v, MediaBrowserCompat.MediaItem song);
    }
}
