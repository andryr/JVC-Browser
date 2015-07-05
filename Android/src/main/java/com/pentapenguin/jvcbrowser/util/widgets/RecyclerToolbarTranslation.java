package com.pentapenguin.jvcbrowser.util.widgets;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class RecyclerToolbarTranslation extends RecyclerView.OnScrollListener {

    private static float THRESHOLD;

    private int mToolbarOffset = 0;
    private boolean mControlsVisible = true;
    private int mToolbarHeight;
    private View mView;

    public RecyclerToolbarTranslation(View view, int toolbarHeight) {
        mToolbarHeight = toolbarHeight;
        THRESHOLD = toolbarHeight/2;
        mView = view;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);

        if(newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (mControlsVisible) {
                if (mToolbarOffset > THRESHOLD) {
                    setInvisible();
                } else {
                    setVisible();
                }
            } else {
                if ((mToolbarHeight - mToolbarOffset) > THRESHOLD) {
                    setVisible();
                } else {
                    setInvisible();
                }
            }
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        clipToolbarOffset();
        ViewCompat.animate(mView).translationY(-mToolbarOffset);

        if((mToolbarOffset < mToolbarHeight && dy > 0) || (mToolbarOffset > 0 && dy < 0)) {
            mToolbarOffset += dy;
        }
    }

    private void clipToolbarOffset() {
        if(mToolbarOffset > mToolbarHeight) {
            mToolbarOffset = mToolbarHeight;
        } else if(mToolbarOffset < 0) {
            mToolbarOffset = 0;
        }
    }

    private void setVisible() {
        if(mToolbarOffset > 0) {
            ViewCompat.animate(mView).translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            mToolbarOffset = 0;
        }
        mControlsVisible = true;
    }

    private void setInvisible() {
        if(mToolbarOffset < mToolbarHeight) {
            ViewCompat.animate(mView).translationY(-mToolbarHeight).setInterpolator(new AccelerateInterpolator(2))
                    .start();
            mToolbarOffset = mToolbarHeight;
        }
        mControlsVisible = false;
    }
}
