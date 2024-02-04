package com.yash.ymplayer.ui.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.ConverterUtil;

public interface PlayerAware {

    String TAG = "PlayerAware";

    View getView();

    default void adjust(int state) {
        View view = getView();
        if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            view.setPadding(0, 0, 0 , (int) ConverterUtil.getPx(view.getContext(), 58));
        } else if (state == BottomSheetBehavior.STATE_HIDDEN) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt((int) ConverterUtil.getPx(view.getContext(), 58), 0);
            valueAnimator.addUpdateListener(valueAnimator1 -> {
                view.setPadding(0, 0, 0, (int) valueAnimator1.getAnimatedValue());
            });
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.start();
        }
    }

    static void adjust(View view, int state) {
        if(view == null) {
            LogHelper.d(TAG, "adjust: view is null");
            return;
        }
        if(view instanceof PlayerAware){
            LogHelper.d(TAG, "adjust: View is the instance");
            ((PlayerAware) view).adjust(state);
        } else if(view instanceof ViewGroup) {
            boolean found = checkInsideViewGroup((ViewGroup) view, state);
            LogHelper.d(TAG, "adjust: View is the instance of ViewGroup, found: %s", found);
        }
    }


    static boolean checkInsideViewGroup(ViewGroup viewGroup, int state) {
        LogHelper.d(TAG, "adjust: View is the instance of ViewGroup");
        var childrenCount = viewGroup.getChildCount();
        for (int i = childrenCount - 1; i >= 0; i--) {
            View child = viewGroup.getChildAt(i);
            if(child instanceof PlayerAware) {
                LogHelper.d(TAG, "adjust: Child is the instance of PlayerAwareRecyclerView");
                ((PlayerAware) child).adjust(state);
                return true;
            } else if (child instanceof ViewGroup) {
                LogHelper.d(TAG, "adjust: Child is the instance of ViewGroup");
                if(checkInsideViewGroup((ViewGroup) child, state)) {
                    return true;
                }
            }
        }
        return false;
    }
}
