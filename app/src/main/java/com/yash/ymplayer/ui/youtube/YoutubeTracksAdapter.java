package com.yash.ymplayer.ui.youtube;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.List;

@Deprecated
public class YoutubeTracksAdapter extends RecyclerView.Adapter<YoutubeTracksAdapter.YouTubeTracksViewHolder> {
    private static final String TAG = "YoutubeTracksAdapter";
    Context context;
    List<YoutubeSong> songs;
    TrackClickListener listener;

    public YoutubeTracksAdapter(Context context, List<YoutubeSong> songs, TrackClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public YouTubeTracksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicBinding binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new YouTubeTracksViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull YouTubeTracksViewHolder holder, int position) {
        holder.bindTracks(songs.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }


    class YouTubeTracksViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        public YouTubeTracksViewHolder(@NonNull ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindTracks(YoutubeSong song, TrackClickListener listener) {
            PopupMenu menu = new PopupMenu(context, binding.more);
            menu.inflate(R.menu.youtube_song_menu);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.play_single:
                        listener.onPlaySingle(song);
                        return true;
                    case R.id.add_to_playlist:
                        listener.addToPlaylist(song);
                        return true;
                    case R.id.download128kbps:
                        listener.download(song, 128);
                        return true;
                    case R.id.download192kbps:
                        listener.download(song, 192);
                        return true;
                    case R.id.download320kbps:
                       listener.download(song, 320);
                        return true;
                    case R.id.queue_next:
                        listener.onQueueNext(song);
                        return true;
                    case R.id.queue_last:
                        listener.onQueueLast(song);
                        return true;

                    default:
                        return false;
                }
            });
            binding.more.setOnClickListener(v -> menu.show());
            binding.title.setText(song.getTitle());
            binding.subTitle.setText(song.getChannelTitle());
            Glide.with(context).load(song.getArt_url_small()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.art);
            itemView.setOnClickListener(v -> listener.onClick(song));
            LogHelper.d(TAG, "bindTracks: %s",  song);
        }
    }
}
