package com.yash.ymplayer.util;

import static com.yash.ymplayer.util.DiffCallbacks.MEDIAITEM_DIFF_CALLBACK;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.interfaces.SongContextMenuListener;
import com.yash.ymplayer.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ItemViewHolder> {
    private static final String TAG = "SongsListAdapter";

    private final AsyncListDiffer<MediaBrowserCompat.MediaItem> asyncListDiffer = new AsyncListDiffer<>(this, MEDIAITEM_DIFF_CALLBACK);
    private final CategoryAdapter.OnItemClickListener listener;
    private final SongsContextMenuClickListener songContextMenuListener;
    private final int mode;
    Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread handlerThread;
    private final Handler workerHandler;

    int size;
    Drawable failedDrawable;
    Pattern pattern;
    ActivityResultLauncher<IntentSenderRequest> launcher;


    @SuppressLint("UseCompatLoadingForDrawables")
    public SongsAdapter(Context context, ActivityResultLauncher<IntentSenderRequest> launcher, CategoryAdapter.OnItemClickListener listener, SongsContextMenuClickListener songContextMenuListener, int mode) {
        this.launcher = launcher;
        this.listener = listener;
        this.songContextMenuListener = songContextMenuListener;
        this.context = context;
        this.mode = mode;
        size = (int) (context.getResources().getDisplayMetrics().density * 50);
        failedDrawable = context.getDrawable(R.drawable.album_art_placeholder);
        pattern = Pattern.compile("[0-9]+");

        handlerThread = new HandlerThread("Artwork");
        handlerThread.start();
        workerHandler = new Handler(handlerThread.getLooper());
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicBinding itemMusicBinding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(itemMusicBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem mediaItem = asyncListDiffer.getCurrentList().get(position);
        holder.bindSongs(mediaItem, listener, songContextMenuListener, mode);
    }


    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        holder.cancelLoadArtJob();
    }

    public void refreshList(List<MediaBrowserCompat.MediaItem> newList) {
        asyncListDiffer.submitList(newList);
    }

    @Override
    public int getItemCount() {
        return asyncListDiffer.getCurrentList().size();
    }

    public void playRandom(View v) {
        // Play a random song from songs list
        int randomIndex = (int) (Math.random() * asyncListDiffer.getCurrentList().size());
        listener.onClick(v, asyncListDiffer.getCurrentList().get(randomIndex));
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemMusicBinding binding;
        private Runnable loadArtJob = null;
        private boolean imageLoaded = false;

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
                            List<MediaBrowserCompat.MediaItem> items = new ArrayList<>(asyncListDiffer.getCurrentList());
                            items.remove(song);
                            refreshList(items);
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

            if(!imageLoaded) {
                loadArtJob = () -> loadArt(this, song);
                workerHandler.postDelayed(loadArtJob, TimeUnit.MILLISECONDS.toMillis(300));
            }
        }

        void cancelLoadArtJob() {
            if (loadArtJob != null) {
                workerHandler.removeCallbacks(loadArtJob);
            }
        }


        private void loadArt(ItemViewHolder itemViewHolder, MediaBrowserCompat.MediaItem song) {
                try {
                    String[] parts = Objects.requireNonNull(song.getDescription().getMediaId()).split("[/|]");
                    boolean isEmbeddedArt = pattern.matcher(parts[parts.length - 1]).matches();
                    Uri artUrl = song.getDescription().getIconUri();
                    if (isEmbeddedArt)
                        artUrl = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(parts[parts.length - 1]));
                    Glide.with(itemViewHolder.binding.art).load(isEmbeddedArt ? CommonUtil.getEmbeddedPicture(context, artUrl) : artUrl).transition(DrawableTransitionOptions.withCrossFade()).into(new CustomTarget<Drawable>(size, size) {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Objects.requireNonNull(transition).transition(resource, new BitmapImageViewTarget(itemViewHolder.binding.art));
                            imageLoaded = true;
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            LogHelper.d(TAG, "onLoadFailed: ");
                            itemViewHolder.binding.art.setImageDrawable(failedDrawable);
                            imageLoaded = true;
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }


    }


    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        handlerThread.interrupt();
    }




    public static class MODE {
        public static final int ALL = 0;
        public static final int ALBUM = 1;
        public static final int ARTIST = 2;
        public static final int PLAYLIST = 3;
    }
}
