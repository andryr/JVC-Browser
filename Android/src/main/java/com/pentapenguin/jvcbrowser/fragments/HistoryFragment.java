package com.pentapenguin.jvcbrowser.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.History;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    public static final String TAG = "history";

    private HistoryAdapter mAdapter;

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new HistoryAdapter();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_link, container, false);
        RecyclerView2 recycler = (RecyclerView2) layout.findViewById(R.id.links_list);

        recycler.setAdapter(mAdapter);
        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources().getString(R.string.subtitle_history));
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        TextView empty = (TextView) layout.findViewById(R.id.link_empty_text);
        empty.setText(R.string.no_history);
        recycler.setLoadingView(empty);
        recycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance((Topic) item), true);
                    }

                    @Override
                    public void onLongClick(Object item, int position) {
                        mAdapter.removeItem(position);
                    }
                }));

        return layout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_history_clear:
                History.reset();
                mAdapter.load();
                return true;
        }
        return false;
    }

    private class HistoryAdapter extends RecyclerViewAdapter<HistoryHolder> {

        private ArrayList<Topic> mValues;

        public HistoryAdapter() {
            mValues = new ArrayList<Topic>();
            load();
        }

        private void load() {
            mValues = History.getList();
            notifyDataSetChanged();
        }

        @Override
        public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_link, parent, false);
            return new HistoryHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void removeItem(int position) {
            History.remove(mValues.get(position));
            notifyDataSetChanged();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }
    }

    private class HistoryHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public HistoryHolder(View view) {
            super(view);
            mContent = (TextView) view.findViewById(R.id.text_link);
        }

        public void bind(final Topic topic) {
            mContent.setText(topic.getContent());
        }
    }
}
