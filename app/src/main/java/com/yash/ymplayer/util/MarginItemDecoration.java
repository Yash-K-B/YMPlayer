package com.yash.ymplayer.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yash.ymplayer.helper.LogHelper;

public class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private int margin;
    private static final String TAG = "MarginItemDecoration";

    public MarginItemDecoration(int marginPx) {
        this.margin = marginPx;
    }

    @Override
    public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int pos = parent.getChildLayoutPosition(view);
        boolean isFirst = ((pos % 2) == 0);

        outRect.left = isFirst ? margin : margin / 2;
        outRect.right = isFirst ? margin / 2 : margin;
        if (pos == 0 || pos == 1) {
            outRect.bottom = margin;
            outRect.top = margin;
        } else {
            outRect.bottom = margin;
        }

    }
}
