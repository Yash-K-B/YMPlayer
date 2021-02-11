package com.yash.ymplayer.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private int margin;
    private static final String TAG = "MarginItemDecoration";

    public MarginItemDecoration(int marginPx) {
        this.margin = marginPx;
    }

    @Override
    public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int pos = parent.getChildLayoutPosition(view);
        //boolean isFirst = ((pos % 2) == 0);
        int position = pos % 3;

        switch (position) {
            case 0:
                outRect.left = margin;
                outRect.right = margin / 2;
                break;
            case 1:
                outRect.left = margin / 2;
                outRect.right = margin / 2;
                break;
            case 2:
                outRect.left = margin / 2;
                outRect.right = margin;
                break;
            default:
        }

        outRect.bottom = margin;
        if (pos == 0 || pos == 1 || pos == 2) {
            outRect.top = margin;
        }

    }
}
