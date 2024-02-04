package com.yash.ymplayer.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class PlayerAwareRecyclerView extends RecyclerView implements PlayerAware {

    private View mView;
    public PlayerAwareRecyclerView(@NonNull Context context) {
        super(context);
    }

    public PlayerAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * Added for embedded default cases handling (When there is no way to replace the view)
     * @param view Recycler view
     */
    public PlayerAwareRecyclerView(@NonNull RecyclerView view) {
        super(view.getContext());
        mView = view;
    }

    @Override
    public View getView() {
        return mView == null ? this : mView;
    }
}
