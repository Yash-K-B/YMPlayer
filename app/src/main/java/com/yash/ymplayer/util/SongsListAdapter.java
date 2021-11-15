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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.main.LocalViewModel;
import com.yash.ymplayer.ui.main.SongContextMenuListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class SongsListAdapter extends RecyclerView.Adapter<SongsListAdapter.ItemViewHolder> {
    private static final String TAG = "debug";
    List<MediaBrowserCompat.MediaItem> songs;
    List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
    private SongListAdapter.OnItemClickListener listener;
    private SongsContextMenuClickListener songContextMenuListener;
    private int mode;
    Context context;
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() > 2 ? Runtime.getRuntime().availableProcessors() - 1 : 1);
    Handler handler = new Handler(Looper.getMainLooper());
    int size;
    Drawable failedDrawable;
    LocalViewModel viewModel;
    Pattern pattern;
    ActivityResultLauncher<IntentSenderRequest> launcher;


    public SongsListAdapter(Context context, ActivityResultLauncher<IntentSenderRequest> launcher, List<MediaBrowserCompat.MediaItem> songs, SongListAdapter.OnItemClickListener listener, SongsContextMenuClickListener songContextMenuListener, int mode) {
        this.songs = songs;
        this.launcher = launcher;
        allSongs.addAll(songs);
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
        holder.bindSongs(songs.get(position), listener, songContextMenuListener, mode);
        //Log.d(TAG, "No of Processors: " + Runtime.getRuntime().availableProcessors());
    }

    public void refreshList() {
        allSongs.clear();
        allSongs.addAll(songs);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        ItemViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindSongs(MediaBrowserCompat.MediaItem song, SongListAdapter.OnItemClickListener listener, SongContextMenuListener songContextMenuListener, int mode) {

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
                        builder.setItems(list, (dialog, which) -> songContextMenuListener.addToPlaylist(song, list[which]));
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return true;
                    case R.id.play_single:
                        songContextMenuListener.playSingle(song);
                        return true;
                    case R.id.queue_next:
                        songContextMenuListener.queueNext(song);
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
                            songs.remove(getAdapterPosition());
                            SongsListAdapter.this.notifyItemRemoved(getAdapterPosition());
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

            executor.execute(() -> {
                if (viewModel.songImages.get(song.getDescription().getMediaId()) == null) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    String[] parts = song.getDescription().getMediaId().split("[/|]");
                    try {
                        boolean isEmbeddedArt = pattern.matcher(parts[parts.length - 1]).matches();
                        if (isEmbeddedArt)
                            retriever.setDataSource(context, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(parts[parts.length - 1])));
                        Glide.with(context).load(isEmbeddedArt ? retriever.getEmbeddedPicture() : song.getDescription().getIconUri()).into(new CustomTarget<Drawable>(size, size) {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                handler.post(() -> binding.art.setImageDrawable(resource));
                                viewModel.songImages.put(song.getDescription().getMediaId(), resource);
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
}
