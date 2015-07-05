package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.Bans;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;

import java.util.ArrayList;

public class BannedFragment extends Fragment {

    public static final String TAG = "banned";

    private BannedAdapter mAdapter;

    public static BannedFragment newInstance() {
        return new BannedFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new BannedAdapter();
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            ((TitleObserver) activity).updateTitle(getActivity().getResources().getString(R.string.subtitle_banned));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_history, container, false);
        RecyclerView2 recycler = (RecyclerView2) layout.findViewById(R.id.history_list);

        recycler.setAdapter(mAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recycler.addOnItemTouchListener(new RecyclerItemListener(mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onLongClick(Object item, int position) {
                        mAdapter.removeItem(position);
                    }
                }));
        TextView empty = (TextView) layout.findViewById(R.id.history_empty_text);
        empty.setText(R.string.no_banned);
        recycler.setLoadingView(empty);

        return layout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_banned, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_banned_clear:
                Bans.reset();
                mAdapter.load();
                return true;
        }
        return false;
    }

    private class BannedAdapter extends RecyclerViewAdapter<BannedHolder> {

        private ArrayList<String> mValues;
        private LayoutInflater mInflater;

        public BannedAdapter() {
            mValues = new ArrayList<String>();
            mInflater = getActivity().getLayoutInflater();
            load();
        }

        private void load() {
            mValues = Bans.getList();
            notifyDataSetChanged();
        }

        @Override
        public BannedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_link, parent, false);
            return new BannedHolder(view);
        }

        @Override
        public void onBindViewHolder(BannedHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void removeItem(int position) {
            Bans.remove(mValues.get(position));
            mValues.remove(position);
            notifyDataSetChanged();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }

    }

    private class BannedHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public BannedHolder(View view) {
            super(view);
            mContent = (TextView) view.findViewById(R.id.text_link);
        }

        public void bind(final String banni) {
            mContent.setText(banni);
        }
    }
}
