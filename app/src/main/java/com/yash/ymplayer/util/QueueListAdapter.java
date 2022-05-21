package com.yash.ymplayer.util;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.ItemPlayingQueueBinding;
import com.yash.logging.LogHelper;

import java.util.List;

public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.QueueItemHolder> {
    private static final String TAG = "QueueListAdapter";
    Context context;
    QueueItemOnClickListener listener;
    List<Song> songs;
    int activePosition = -1;
    int prevActivePosition = -1;
    int color;

    public QueueListAdapter(Context context, QueueItemOnClickListener listener, List<Song> songs) {
        this.context = context;
        this.listener = listener;
        this.songs = songs;
        this.color = getAttributeColor(R.attr.listTitleTextColor);
    }

    @NonNull
    @Override
    public QueueItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlayingQueueBinding binding = ItemPlayingQueueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new QueueItemHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueItemHolder holder, int position) {
        songs.get(position).setColor((position == activePosition) ? Color.GREEN : color);
        holder.bindQueueItem(songs.get(position), listener, holder);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }



    static class QueueItemHolder extends RecyclerView.ViewHolder {
        ItemPlayingQueueBinding binding;

        public QueueItemHolder(ItemPlayingQueueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindQueueItem(Song song, QueueItemOnClickListener listener, RecyclerView.ViewHolder holder) {
            binding.trackName.setText(song.getTitle());
            binding.trackName.setTextColor(song.getColor());
            binding.removeFromQueue.setOnClickListener(v -> {
                binding.removeFromQueue.setOnClickListener(null);
                listener.onDelete(song);
            });
            binding.dragToArrange.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        listener.startDrag(holder);
                    }
                    return false;
                }
            });
            binding.trackName.setOnClickListener(v -> listener.onClick(song));
        }
    }

    private int getAttributeColor(int resId) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(resId, value, true);
        return value.data;
    }


    public void notifyQueueChange(int activePosition) {
        this.activePosition = activePosition;
        notifyDataSetChanged();
    }

    public void notifyItemMoved(int fromPosition, int toPosition, int activePosition) {
        this.activePosition = activePosition;
        notifyItemMoved(fromPosition, toPosition);
    }
    public void notifyItemDeleted(int pos) {
        if(activePosition != pos)
            if(pos<activePosition) activePosition--;
        notifyItemRemoved(pos);
    }

    public void notifyActiveItem(int currentPosition) {
        if (songs.size() <= currentPosition)
            activePosition = currentPosition;
        else {
            activePosition = currentPosition;
            LogHelper.d(TAG, "notifyActiveItem: "+currentPosition);
            notifyItemChanged(currentPosition);
        }
    }

    public void invalidateItem(int previousPlayingPosition) {
        if (songs.size() <= previousPlayingPosition) return;
        LogHelper.d(TAG, "invalidateItem: "+previousPlayingPosition);
        notifyItemChanged(previousPlayingPosition);
    }


    public interface QueueItemOnClickListener {
        /**
         * Provide Click Event to the Queue Items
         *
         * @param song current queue element
         */
        void onClick(Song song);

        /**
         * Provide Delete Button Click Event to the Queue Items
         *
         * @param song current queue element
         */
        void onDelete(Song song);

        /**
         * Callback for Drag Started Event
         *
         * @param viewHolder current ViewHolder
         */
        void startDrag(RecyclerView.ViewHolder viewHolder);
    }
}
