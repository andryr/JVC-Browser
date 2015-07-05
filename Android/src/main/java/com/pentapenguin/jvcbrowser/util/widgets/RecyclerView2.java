package com.pentapenguin.jvcbrowser.util.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class RecyclerView2 extends RecyclerView {

    private View mEmptyView;
    private View mLoadingView;

    public RecyclerView2(Context context) { super(context); }

    public RecyclerView2(Context context, AttributeSet attrs) { super(context, attrs); }

    public RecyclerView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void checkIfLoading() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        if (mLoadingView != null) {
            mLoadingView.setVisibility(getAdapter().getItemCount() > 0 ? GONE : VISIBLE);
        }
    }

    final AdapterDataObserver observer = new AdapterDataObserver() {
        @Override public void onChanged() {
            super.onChanged();
            checkIfLoading();
        }
    };

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
    }

    public void setEmptyView(@Nullable View emptyView) {
        mEmptyView = emptyView;
    }

    public void setLoadingView(View loadingView) {
        mLoadingView = loadingView;
        checkIfLoading();
    }

    public void showNoResults() {
        if (getAdapter().getItemCount() == 0) {
            if (mEmptyView != null) mEmptyView.setVisibility(View.VISIBLE);
            if (mLoadingView != null) mLoadingView.setVisibility(View.GONE);
        }
    }

}