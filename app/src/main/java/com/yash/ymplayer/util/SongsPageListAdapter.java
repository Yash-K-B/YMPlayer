package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.interfaces.SongContextMenuListener;
import com.yash.ymplayer.pool.ThreadPool;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.main.LocalViewModel;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class SongsPageListAdapter extends PagingDataAdapter<MediaBrowserCompat.MediaItem, SongsPageListAdapter.ItemViewHolder> {
    private static final String TAG = "SongsListAdapter";
    private final CategoryAdapter.OnItemClickListener listener;
    private final SongsContextMenuClickListener songContextMenuListener;
    private final int mode;
    Context context;
    Handler handler = new Handler(Looper.getMainLooper());
    int size;
    Drawable failedDrawable;
    LocalViewModel viewModel;
    Pattern pattern;
    ActivityResultLauncher<IntentSenderRequest> launcher;


    public SongsPageListAdapter(Context context, ActivityResultLauncher<IntentSenderRequest> launcher, CategoryAdapter.OnItemClickListener listener, SongsContextMenuClickListener songContextMenuListener, int mode) {
        super(DiffCallback);
        this.launcher = launcher;
        this.listener = listener;
        this.songContextMenuListener = songContextMenuListener;
        this.context = context;
        this.mode = mode;
        size = (int) (context.getResources().getDisplayMetrics().density * 50);
        failedDrawable = context.getDrawable(R.drawable.album_art_placeholder);
        pattern = Pattern.compile("[0-9]+");
    }

    public void setViewModel(LocalViewModel viewModel) {
        this.viewModel = viewModel;
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicBinding itemMusicBinding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(itemMusicBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bindSongs(getItem(position), listener, songContextMenuListener, mode);
        //Log.d(TAG, "No of Processors: " + Runtime.getRuntime().availableProcessors());
    }

//    public void refreshList(List<MediaBrowserCompat.MediaItem> newList) {
//        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(this.songs, newList));
//        songs.clear();
//        songs.addAll(newList);
//        diffResult.dispatchUpdatesTo(this);
//    }
//
//    public void playRandom(View v) {
//        // Play a random song from songs list
//        int randomIndex = (int) (Math.random() * songs.size());
//        listener.onClick(v, songs.get(randomIndex));
//    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        ItemViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindSongs(MediaBrowserCompat.MediaItem song, CategoryAdapter.OnItemClickListener listener, SongContextMenuListener songContextMenuListener, int mode) {

            PopupMenu menu = new PopupMenu(context, binding.more);
            switch (mode) {
                case MODE.ALBUM:
                    menu.inflate(R.menu.album_songs_context_menu);
                    break;
                case MODE.ARTIST:
                    menu.inflate(R.menu.artist_songs_context_menu);
                    break;
                case MODE.PLAYLIST:
                    menu.inflate(R.menu.playlist_songs_context_menu);
                    break;
                default:
                    menu.inflate(R.menu.song_context_menu);
            }

            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.add_to_playlist:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Choose Playlist");
                        List<MediaBrowserCompat.MediaItem> lists = Repository.getInstance(context).getAllPlaylists();
                        String[] list = new String[lists.size()];
                        for (int i = 0; i < lists.size(); i++) {
                            list[i] = lists.get(i).getDescription().getTitle().toString();
                        }
                        builder.setItems(list, (dialog, which) -> {
                            Keys.PlaylistType playlistType = Keys.PlaylistType.HYBRID_PLAYLIST.name().contentEquals(lists.get(which).getDescription().getDescription())? Keys.PlaylistType.HYBRID_PLAYLIST: Keys.PlaylistType.PLAYLIST;
                            songContextMenuListener.addToPlaylist(song, list[which], playlistType);
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return true;
                    case R.id.play_single:
                        songContextMenuListener.playSingle(song);
                        return true;
                    case R.id.queue_next:
                        songContextMenuListener.queueNext(song);
                        return true;
                    case R.id.queue_last:
                        songContextMenuListener.queueLast(song);
                        return true;
                    case R.id.go_to_album:
                        songContextMenuListener.gotoAlbum(song);
                        return true;
                    case R.id.goto_artist:
                        songContextMenuListener.gotoArtist(song);
                        return true;
                    case R.id.shareSong:
                        songContextMenuListener.shareSong(song);
                        return true;
                    case R.id.deleteFromStorage:
                        if (songContextMenuListener.deleteFromStorage(song, launcher)) {
//                            get.remove(getAbsoluteAdapterPosition());
                            SongsPageListAdapter.this.notifyItemRemoved(getAbsoluteAdapterPosition());
                        }
                        return true;
                    default:
                        return false;
                }

            });

            binding.more.setOnClickListener(v -> menu.show());
            binding.title.setText(song.getDescription().getTitle());
            binding.subTitle.setText(song.getDescription().getSubtitle());
            itemView.setOnClickListener(v -> listener.onClick(v, song));

            ThreadPool.getInstance().getExecutor().submit(() -> {
                if (viewModel.songImages.get(song.getDescription().getMediaId()) == null) {
                    String[] parts = Objects.requireNonNull(song.getDescription().getMediaId()).split("[/|]");
                    try {
                        boolean isEmbeddedArt = pattern.matcher(parts[parts.length - 1]).matches();
                        Uri artUrl = song.getDescription().getIconUri();
                        if (isEmbeddedArt)
                            artUrl = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(parts[parts.length - 1]));
                        Glide.with(binding.art).load(isEmbeddedArt ? CommonUtil.getEmbeddedPicture(context, artUrl) : artUrl).transition(DrawableTransitionOptions.withCrossFade()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(new CustomTarget<Drawable>(size, size) {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                viewModel.songImages.put(song.getDescription().getMediaId(), resource);
                                handler.post(() -> {
                                    if(!Objects.requireNonNull(transition).transition(resource, new BitmapImageViewTarget(binding.art))) {
                                        binding.art.setImageDrawable(resource);
                                    }
                                });
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                handler.post(() -> binding.art.setImageDrawable(failedDrawable));
                                viewModel.songImages.put(song.getDescription().getMediaId(), failedDrawable);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    handler.post(() -> {
                        binding.art.setImageDrawable(viewModel.songImages.get(song.getDescription().getMediaId()));
                    });
                }

            });

        }
    }

    public static class MODE {
        public static final int ALL = 0;
        public static final int ALBUM = 1;
        public static final int ARTIST = 2;
        public static final int PLAYLIST = 3;
    }

    private static final DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem> DiffCallback = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaBrowserCompat.MediaItem oldItem, @NonNull MediaBrowserCompat.MediaItem newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaBrowserCompat.MediaItem oldItem, @NonNull MediaBrowserCompat.MediaItem newItem) {
            return oldItem.getMediaId() != null && newItem.getMediaId() != null && oldItem.getMediaId().equals(newItem.getMediaId());
        }
    };
}
