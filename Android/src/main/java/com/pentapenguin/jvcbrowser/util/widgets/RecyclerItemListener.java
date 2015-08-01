package com.pentapenguin.jvcbrowser.util.widgets;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.pentapenguin.jvcbrowser.app.App;

public class RecyclerItemListener implements RecyclerView.OnItemTouchListener {

    private RecyclerItemGestureListener mListener;
    private RecyclerViewAdapter mAdapter;
    private GestureDetectorCompat mGestureDetector;
    private int mPosition;

    public RecyclerItemListener(Context context, RecyclerViewAdapter adapter, RecyclerItemGestureListener listener) {
        mListener = listener;
        mAdapter = adapter;
        mGestureDetector = new GestureDetectorCompat(context, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onDown(MotionEvent motionEvent) {
                mListener.onDown(mAdapter.itemAt(mPosition), mPosition);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {
                mListener.onShowPress(mAdapter.itemAt(mPosition), mPosition);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                mListener.onClick(mAdapter.itemAt(mPosition), mPosition);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                mListener.onScroll(motionEvent, motionEvent1, v, v1);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                mListener.onLongClick(mAdapter.itemAt(mPosition), mPosition);

            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                mListener.onFling(motionEvent, motionEvent1, v, v1);
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View child = view.findChildViewUnder(e.getX(), e.getY());
        if (mListener == null || mAdapter == null) throw new NullPointerException();

        if (child != null) {
            mPosition = view.getChildAdapterPosition(child);
            mGestureDetector.onTouchEvent(e);
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    public static class RecyclerItemGestureListener {

        public void onDown(Object item, int position) {

        }

        public void onShowPress(Object item, int position) {

        }

        public void onClick(Object item, int position) {

        }

        public void onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {

        }

        public void onLongClick(Object item, int position) {

        }

        public void onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {

        }
    }
}