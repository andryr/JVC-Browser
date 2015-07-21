package com.pentapenguin.jvcbrowser.util.widgets;

public abstract class RecyclerViewAdapter<VH extends android.support.v7.widget.RecyclerView.ViewHolder>
        extends RecyclerView2.Adapter<VH> {

    public abstract Object itemAt(int position);


}
