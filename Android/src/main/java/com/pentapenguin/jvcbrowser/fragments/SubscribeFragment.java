package com.pentapenguin.jvcbrowser.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SubscribeFragment extends Fragment {

    public static final String TAG = "subscribe";
    private static final String URL = "http://www.jeuxvideo.com/profil/{?}?mode=notifications";
    private static final String DATA_SAVE = "themes";

    private SubscribeAdapter mAdapter;
    private String mTimestamp;
    private String mHash;
    private RecyclerView2 mRecycler;

    public static SubscribeFragment newInstance() {
        return new SubscribeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.subscribeFragment, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.links_list);

        if (savedInstanceState != null) {
            ArrayList<Topic> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mAdapter = new SubscribeAdapter(data);
        } else {
            mAdapter = new SubscribeAdapter();
        }

        mRecycler.setAdapter(mAdapter);
        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources()
                .getString(R.string.subtitle_subscribe));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        TextView empty = (TextView) layout.findViewById(R.id.link_empty_text);
        empty.setText(R.string.no_subscribes);
        mRecycler.setLoadingView(layout.findViewById(R.id.link_loading_bar));
        mRecycler.setEmptyView(empty);
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance((Topic) item), true);
                    }

                    @Override
                    public void onLongClick(final Object item, final int position) {

                        App.alertOkCancel(getActivity(), "Supprimer cet abonnement ?",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        unsubscribe((Topic) item, position);
                                    }
                                });
                    }
                }));

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.mValues);
    }

    private void unsubscribe(final Topic topic, final int position) {
        String url = "http://www.jeuxvideo.com/abonnements/ajax/ajax_abo_delete.php";
        HashMap<String, String> data = new HashMap<String, String>();

        data.put("ajax_timestamp", mTimestamp);
        data.put("ajax_hash", mHash);
        data.put("id", Integer.toString(topic.getPage()));
        Ajax.url(url).post().data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                .ignoreContentType(true).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                if (response != null) {
                    String result = json(response.body());
                    if (result == null) {
                        App.toast(R.string.subscribe_deleted);
                        mAdapter.removeAt(position);
                        return;
                    }
                    App.alert(getActivity(), result);
                } else {
                    App.alert(getActivity(), R.string.no_response);
                }
            }

            private String json(String message) {
                try {
                    JSONObject json = new JSONObject(message);
                    String error = json.getString("error");
                    if (!error.equals("")) return error;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).execute();
    }

    private class SubscribeAdapter extends RecyclerViewAdapter<SubscribeHolder> {

        private ArrayList<Topic> mValues;

        public SubscribeAdapter() {
            mValues = new ArrayList<Topic>();
            load();
        }

        public SubscribeAdapter(ArrayList<Topic> values) {
            mValues = new ArrayList<Topic>(values);
        }

        private void load() {
            String url = URL.replace("{?}", Auth.getInstance().getPseudo().toLowerCase()).trim();

            url = url.replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            String[] infos = Parser.subscribeData(doc).split("#");
                            mTimestamp = infos[0];
                            mHash = infos[1];
                            mValues = Parser.subscribes(doc);
                            if (!mValues.isEmpty()) {
                                notifyDataSetChanged();
                            } else {
                                mRecycler.showNoResults();
                            }
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                    } else {
                        App.snack(getView(), R.string.no_response);
                    }
                }
            }).execute();
        }

        @Override
        public Object itemAt(int position) {
            return mValues.get(position);
        }

        @Override
        public SubscribeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(Theme.link, parent, false);
            return new SubscribeHolder(view);
        }

        @Override
        public void onBindViewHolder(SubscribeHolder holder, int position) {
        holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void removeAt(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
        }
    }

    private class SubscribeHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public SubscribeHolder(View view) {
            super(view);
            mContent = (TextView) view.findViewById(R.id.text_link);
        }

        public void bind(Topic topic) {
            mContent.setText(topic.getContent());
        }
    }
}
