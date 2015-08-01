package com.pentapenguin.jvcbrowser.fragments;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.ArrayList;

public class ForumSearchFragment extends Fragment {

    public static final String TAG = "forum_search";

    public static ForumSearchFragment newInstance() {
        return new ForumSearchFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_forum_search, container, false);
        final RecyclerView2 recycler = (RecyclerView2) layout.findViewById(R.id.forum_search_list);
        final EditText forumName = (EditText) layout.findViewById(R.id.forum_search_name);
        Button search = (Button) layout.findViewById(R.id.forum_search_button);
        final ForumSearchAdapter adapter = new ForumSearchAdapter();

        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources()
                .getString(R.string.subtitle_search_forum));
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), adapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        ((FragmentLauncher) getActivity()).launch(ForumFragment.newInstance((Forum) item), true);
                    }
                }));
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = checkField(forumName);

                if (!name.equals("")) {
                    App.hideKeyboard(forumName.getWindowToken());
                    adapter.load(name);
                    return;
                }
                App.toast(R.string.field_empty);
            }

            private String checkField(EditText edit) {
                if (edit.getText() != null && !edit.getText().toString().equals(""))
                    return edit.getText().toString();
                return "";
            }
        });

        return layout;
    }

    private class ForumSearchAdapter extends RecyclerViewAdapter<ForumSearchHolder> {

        private ArrayList<Forum> mValues;

        public ForumSearchAdapter() {
            mValues = new ArrayList<Forum>();
        }

        public void load(String forumName) {
            String url = "http://api.jeuxvideo.com/forums/search_forum.php?input_search_forum=" + forumName;
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            mValues.clear();
            notifyDataSetChanged();
            Ajax.url(url).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    dialog.dismiss();
                    if (response != null) {
                        try {
                            mValues = Parser.searchForum(response.parse());
                            notifyDataSetChanged();
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
        public ForumSearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_link, parent, false);
            return new ForumSearchHolder(view);
        }

        @Override
        public void onBindViewHolder(ForumSearchHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }
    }

    private class ForumSearchHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public ForumSearchHolder(View itemView) {
            super(itemView);

            mContent = (TextView) itemView.findViewById(R.id.text_link);
        }

        public void bind(final Forum forum) {
            mContent.setText(forum.getContent());

        }
    }
}

