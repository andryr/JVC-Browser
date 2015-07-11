package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.TextView;
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ForumListFragment extends Fragment {

    public static final String TAG = "forum_list";
    public static final String URL = "http://api.jeuxvideo.com/forums.htm";

    public static ForumListFragment newInstance() {
        return new ForumListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_forum_list, container, false);

        RecyclerView mRecycler = (RecyclerView) layout.findViewById(R.id.list_forum_list);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecycler.setHasFixedSize(false);
        RecyclerViewExpandableItemManager mRecyclerViewExpandableItemManager = new RecyclerViewExpandableItemManager(null);
        ForumListAdapter mAdapter = new ForumListAdapter();
        RecyclerView.Adapter mWrappedAdapter = mRecyclerViewExpandableItemManager.createWrappedAdapter(mAdapter);
        mRecycler.setLayoutManager(mLayoutManager);

        mRecycler.setAdapter(mWrappedAdapter);
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerViewExpandableItemManager.attachRecyclerView(mRecycler);
        ((TitleObserver) getActivity()).updateTitle(getResources().getString(R.string.subtitle_all_forum));

        return layout;
    }

    private class ForumListAdapter extends AbstractExpandableItemAdapter<ListGroupHolder, ListChildHolder> {

        private HashMap<String, ArrayList<Forum>> mValues;
        private ArrayList<String> mArray;

        public ForumListAdapter() {
            mValues = new HashMap<String, ArrayList<Forum>>();
            mArray = new ArrayList<String>();
            setHasStableIds(true);
            load();
        }

        public void load() {
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            Ajax.url(URL).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    dialog.dismiss();
                    if (response != null) {
                        try {
                            mValues = Parser.listForums(response.parse());
                            for (String s : mValues.keySet()) {
                                mArray.add(s);
                            }
                            notifyDataSetChanged();
                        } catch (NoContentFoundException e) {
                            App.alert(getActivity(), e.getMessage());
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                    } else {
                        App.alert(getActivity(), R.string.no_response);
                    }
                }
            }).execute();
        }

        @Override
        public int getGroupCount() {
            return mArray.size();
        }

        @Override
        public int getChildCount(int i) {
            return mValues.get(mArray.get(i)).size();
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1;
        }

        @Override
        public int getGroupItemViewType(int i) {
            return 0;
        }

        @Override
        public int getChildItemViewType(int i, int i1) {
            return 0;
        }

        @Override
        public ListGroupHolder onCreateGroupViewHolder(ViewGroup viewGroup, int i) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_forum_list_group, viewGroup, false);
            return new ListGroupHolder(view);
        }

        @Override
        public ListChildHolder onCreateChildViewHolder(ViewGroup viewGroup, int i) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_forum_list_child, viewGroup, false);
            return new ListChildHolder(view);
        }

        @Override
        public void onBindGroupViewHolder(ListGroupHolder listGroupHolder, int groupPosition, int viewType) {
            listGroupHolder.bind(mArray.get(groupPosition));
        }

        @Override
        public void onBindChildViewHolder(ListChildHolder listChildHolder, int groupPosition, int childPosition, int viewType) {
            listChildHolder.bind(mValues.get(mArray.get(groupPosition)).get(childPosition));
        }

        @Override
        public boolean onCheckCanExpandOrCollapseGroup(ListGroupHolder listGroupHolder, int i, int i1, int i2, boolean b) {
            return true;
        }
    }

    private class ListGroupHolder extends AbstractExpandableItemViewHolder {

        private TextView mContent;

        public ListGroupHolder(View itemView) {
            super(itemView);

            mContent = (TextView) itemView.findViewById(R.id.item_forum_list_group_content);
        }


        public void bind(String s) {
            mContent.setText(s);
        }
    }

    private class ListChildHolder extends AbstractExpandableItemViewHolder {

        private TextView mContent;
        private View mFrame;

        public ListChildHolder(View itemView) {
            super(itemView);

            mContent = (TextView) itemView.findViewById(R.id.item_forum_list_child_content);
            mFrame = itemView.findViewById(R.id.item_child_frame);
        }

        public void bind(final Forum forum) {
            mContent.setText(forum.getContent());
            mFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((FragmentLauncher) getActivity()).launch(ForumFragment.newInstance(forum), true);
                }
            });

        }
    }

}
