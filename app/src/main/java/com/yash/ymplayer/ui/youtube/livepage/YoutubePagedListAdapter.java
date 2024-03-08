package com.yash.ymplayer.ui.youtube.livepage;

import android.content.Context;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemMusicBinding;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.util.YoutubeSong;

public class YoutubePagedListAdapter extends PagingDataAdapter<YoutubeSong, YoutubePagedListAdapter.TopTracksViewHolder> {
    private static final String TAG = "YoutubePagedListAdapter";
    TrackClickListener listener;
    MediaControllerCompat mediaController;
    Context context;

    public YoutubePagedListAdapter(Context context, TrackClickListener listener, MediaControllerCompat mediaController) {
        super(DiffCallback);
        this.listener = listener;
        this.context = context;
        this.mediaController = mediaController;
    }

    @NonNull
    @Override
    public TopTracksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMusicBinding binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new TopTracksViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopTracksViewHolder holder, int position) {
        YoutubeSong song = getItem(position);
        if (song != null)
            holder.onBindTracks(song, listener,mediaController);
        if(getItemCount() - 1 == position) {
            LogHelper.d(TAG, "onBindViewHolder: End of the list");
        }
    }

    public void playRandom() {
        int itemCount = getItemCount();
        if(itemCount < 1)
            return;
        int randomIndex = (int) (Math.random() * itemCount);
        listener.onClick(getItem(randomIndex));
    }

    class TopTracksViewHolder extends RecyclerView.ViewHolder {
        ItemMusicBinding binding;

        public TopTracksViewHolder(ItemMusicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void onBindTracks(YoutubeSong song, TrackClickListener listener,MediaControllerCompat  mediaController) {
            PopupMenu menu = new PopupMenu(context,binding.more);
            menu.inflate(R.menu.youtube_song_menu);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()){
                    case R.id.play_single:
                       listener.onPlaySingle(song);
                        return true;
                    case R.id.add_to_playlist:
                        listener.addToPlaylist(song);
                        return true;
                    case R.id.download128kbps:
                        listener.download(song,128);
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

                    default: return false;
                }
            });
            binding.title.setText(song.getTitle());
            binding.subTitle.setText(song.getChannelTitle());
            binding.more.setOnClickListener(v -> menu.show());
            Glide.with(context).load(song.getArt_url_small()).transition(DrawableTransitionOptions.withCrossFade()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.art);
            itemView.setOnClickListener(v -> listener.onClick(song));
        }
    }


    private static final DiffUtil.ItemCallback<YoutubeSong> DiffCallback = new DiffUtil.ItemCallback<YoutubeSong>() {
        @Override
        public boolean areItemsTheSame(@NonNull YoutubeSong oldItem, @NonNull YoutubeSong newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull YoutubeSong oldItem, @NonNull YoutubeSong newItem) {
            return oldItem.getVideoId().equals(newItem.getVideoId());
        }
    };
}
