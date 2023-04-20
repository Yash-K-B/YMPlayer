package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.CreatePlaylistBinding;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.ymplayer.databinding.ItemPlaylistBinding;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.interfaces.AlbumOrArtistContextMenuListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.yash.ymplayer.interfaces.AlbumOrArtistContextMenuListener.*;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongViewHolder> {
    private static final String TAG = "debug";

    List<MediaBrowserCompat.MediaItem> songs;
    List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
    private OnItemClickListener listener;
    private Mode mode;
    private Context context;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());
    AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener;

    public SongListAdapter(Context context, List<MediaBrowserCompat.MediaItem> songs, OnItemClickListener listener, AlbumOrArtistContextMenuListener albumOrArtistContextMenuListener, Mode mode) {
        this.songs = songs;
        this.listener = listener;
        this.mode = mode;
        this.context = context;
        this.albumOrArtistContextMenuListener = albumOrArtistContextMenuListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mode == Mode.ALBUM || mode == Mode.ARTIST) {
            ItemMusicBinding itemMusicBinding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ItemViewHolder(itemMusicBinding);
        } else if (mode == Mode.PLAYLIST) {
            ItemPlaylistBinding itemPlaylistBinding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PlaylistViewHolder(itemPlaylistBinding);
        } else {
            ItemPlayingQueueBinding itemPlayingQueueBinding = ItemPlayingQueueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PlayingQueueViewHolder(itemPlayingQueueBinding);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        if (mode == Mode.ALBUM)
            ((ItemViewHolder) holder).bindSongs(songs.get(position), listener);
        else if (mode == Mode.ARTIST)
            ((ItemViewHolder) holder).bindArtists(songs.get(position), listener, albumOrArtistContextMenuListener);
        else if (mode == Mode.PLAYLIST)
            ((PlaylistViewHolder) holder).bindPlaylist(songs.get(position), listener);
        else
            ((PlayingQueueViewHolder) holder).bindQueueItem(songs.get(position), listener);


    }

    @Override
    public int getItemViewType(int position) {
        return mode.value;
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
            else if ("Last Played".equals(song.getDescription().getTitle() + ""))
                binding.iconPlaylist.setImageResource(R.drawable.icon_last_added);
            else {
                PopupMenu menu = new PopupMenu(context, binding.more);
                menu.inflate(R.menu.playlist_context_menu_user);
                menu.setOnMenuItemClickListener(item -> {
                    ContentResolver resolver = context.getContentResolver();
                    String where = MediaStore.Audio.Playlists._ID + "=?";
                    switch (item.getItemId()) {
                        case R.id.rename:
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            CreatePlaylistBinding playlistBinding = CreatePlaylistBinding.inflate(LayoutInflater.from(context));
                            playlistBinding.supportYoutube.setVisibility(View.GONE);
                            builder.setTitle("Rename playlist")
                                    .setView(playlistBinding.getRoot())
                                    .setPositiveButton("SAVE", (dialog, which) -> {
                                        String newName = String.valueOf(playlistBinding.playlistName.getText());
                                        if(song.getMediaId().startsWith(Keys.PlaylistType.HYBRID_PLAYLIST.name())) {
                                            Repository.getInstance(context).renamePlaylist(CommonUtil.extractIntegerId(song.getMediaId()), newName);
                                        } else {
                                            ContentValues values = new ContentValues();
                                            LogHelper.d(TAG, "bindPlaylist: Playlist renamed to " + newName);
                                            values.put(MediaStore.Audio.Playlists.NAME, newName);
                                            Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                                            resolver.update(uri, values, where, new String[]{CommonUtil.extractId(song.getMediaId())});
                                        }
                                        simulateRenamePlaylist(newName, getAdapterPosition());
                                        notifyItemChanged(getAdapterPosition());
                                    })
                                    .setNegativeButton("CANCEL", (dialog, which) -> {
                                        dialog.dismiss();
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return true;
                        case R.id.remove:
                            if(song.getMediaId().startsWith(Keys.PlaylistType.HYBRID_PLAYLIST.name())) {
                                Repository.getInstance(context).deletePlaylist(CommonUtil.extractIntegerId(song.getMediaId()));
                            } else {
                                resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, new String[]{CommonUtil.extractId(song.getMediaId())});
                            }
                            songs.remove(getAbsoluteAdapterPosition());
                            notifyItemRemoved(getAbsoluteAdapterPosition());
                            Toast.makeText(context, "Playlist " + song.getDescription().getTitle() + " has been deleted successfully", Toast.LENGTH_SHORT).show();
                            return true;
                        default:
                            return false;
                    }
                });
                binding.more.setOnClickListener(v -> menu.show());
                if(song.getDescription().getDescription() == null)
                    binding.iconPlaylist.setImageResource(R.drawable.playlist_default);
                else
                    binding.iconPlaylist.setImageResource(R.drawable.playlist_hybrid);
            }
            binding.playlistTitle.setText(song.getDescription().getTitle());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v, song);
                }
            });
        }
    }

    private void simulateRenamePlaylist(String newName, int adapterPosition) {
        MediaBrowserCompat.MediaItem mediaItem = songs.get(adapterPosition);
        MediaBrowserCompat.MediaItem song = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(mediaItem.getMediaId())
                .setTitle(newName)
                .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
        songs.set(adapterPosition, song);
    }

    class ItemViewHolder extends SongViewHolder {
        ItemMusicBinding binding;

        public ItemViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindSongs(MediaBrowserCompat.MediaItem song, OnItemClickListener listener) {

            String[] parts = song.getDescription().getMediaId().split("[/|]");
            long id = Long.parseLong(parts[parts.length - 1]);
            Log.d(TAG, "run: id: " + id);
            Glide.with(context).load(CommonUtil.getEmbeddedPicture(context, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))).placeholder(R.drawable.album_art_placeholder).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    handler.post(() -> binding.art.setImageDrawable(resource));
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    Log.d(TAG, "onLoadCleared: ");
                }
            });

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
                    menu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.play:
                                albumOrArtistContextMenuListener.play(song, ITEM_TYPE.ARTISTS);
                                return true;
                            case R.id.queue_next:
                                albumOrArtistContextMenuListener.queueNext(song, ITEM_TYPE.ARTISTS);
                                return true;
                            case R.id.add_to_playlist:
                                albumOrArtistContextMenuListener.addToPlaylist(song, ITEM_TYPE.ARTISTS);
                                return true;
                            default:
                                return false;
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

    public enum Mode {
        ALBUM(0), ARTIST(1), PLAYLIST(2);
        private final int value;

        Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
