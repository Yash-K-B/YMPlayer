package com.yash.ymplayer.ui.youtube.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.yash.ymplayer.R;

public class LoadStateFooterAdapter extends LoadStateAdapter<LoadStateFooterAdapter.LoadStateViewHolder> {

    @NonNull
    @Override
    public LoadStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @NonNull LoadState loadState) {
        // Inflate the layout for the load state footer
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.load_state_footer, parent, false);
        return new LoadStateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoadStateViewHolder holder, @NonNull LoadState loadState) {
        // Bind the load state to the view
        ProgressBar progressBar = holder.itemView.findViewById(R.id.progress_bar);
        if (loadState instanceof LoadState.Loading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    static class LoadStateViewHolder extends RecyclerView.ViewHolder {
        public LoadStateViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
