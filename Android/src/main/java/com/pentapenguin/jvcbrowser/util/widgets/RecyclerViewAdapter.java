package com.pentapenguin.jvcbrowser.util.widgets;

import android.support.v7.widget.RecyclerView;

public abstract class RecyclerViewAdapter<VH extends android.support.v7.widget.RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    public abstract Object itemAt(int position);


}
