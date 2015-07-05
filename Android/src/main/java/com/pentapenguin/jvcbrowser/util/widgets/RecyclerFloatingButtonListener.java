package com.pentapenguin.jvcbrowser.util.widgets;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import com.pentapenguin.jvcbrowser.app.App;

public class RecyclerFloatingButtonListener extends RecyclerView.OnScrollListener {

    private boolean mVisible = true;
    private View mView;

    public RecyclerFloatingButtonListener(View view) {
        mView = view;
    }

    private int dip2Pixel(int dipValue){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue,
                App.getContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onScrolled(android.support.v7.widget.RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (dy > 0) { //scroll up
            if (mVisible) {
                mVisible = false;
                animateButton(100);
            }
        } else { //scroll down
            if (!mVisible) {
                mVisible = true;
                animateButton(dip2Pixel(8));
            }
        }
    }

    private void animateButton(final int dy) {
        ViewCompat.animate(mView).translationY(dy).setDuration(1000).start();
    }
}
