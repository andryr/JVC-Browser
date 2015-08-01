package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
import com.pentapenguin.jvcbrowser.entities.*;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteFragment extends Fragment {

    public static final String TAG = "favorite";
    private static final String ARG_FAVORITE = "favorite_type";
    private static final String URL = "http://m.jeuxvideo.com/forums/favoris.php";
    private static final String DATA_SAVE = "data";

    public enum ListType { Forum, Topic}

    private RecyclerView2 mRecycler;
    private FavoriteAdapter mAdapter;
    private ListType mType;

    public static FavoriteFragment newInstance(ListType type) {
        FavoriteFragment fragment = new FavoriteFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FAVORITE, type.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = ListType.values()[getArguments().getInt(ARG_FAVORITE)];

        if (savedInstanceState != null) {
            ArrayList<Link> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mAdapter = new FavoriteAdapter(data);
        } else {
            mAdapter = new FavoriteAdapter();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.getData());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_link, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.links_list);

        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.setEmptyView(layout.findViewById(R.id.link_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.link_loading_bar));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        Link link = (Link) item;
                        Item itemm = Helper.urlResolve(link.getUrl());

                        if (itemm instanceof Forum) {
                            ((FragmentLauncher) getActivity()).launch(ForumFragment.newInstance((Forum) itemm), true);
                        } else if (itemm instanceof Topic) {
                            ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance((Topic) itemm), true);
                        }
                    }

                    @Override
                    public void onLongClick(final Object item, final int position) {
                        final Link link = (Link) item;

                        App.alertOkCancel(getActivity(), "Supprimer des favoris ?",
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Item item = Helper.urlResolve(link.getUrl());
                                if (mType == ListType.Topic && item instanceof Topic) {
                                    deleteTopic((Topic) item, position);
                                } else if (mType == ListType.Forum && item instanceof Forum) {
                                    deleteForum((Forum) Helper.urlResolve(link.getUrl()), position);
                                }
                                dialogInterface.dismiss();
                            }
                        });
                    }

                    private void deleteForum(final Forum forum, final int position) {
                        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

                        dialog.show();
                        String url = App.HOST_WEB + Helper.forumToRaw(forum);
                        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                .callback(new AjaxCallback() {
                                    @Override
                                    public void onComplete(Connection.Response response) {
                                        if (response != null) {
                                            String html = response.body();
                                            String url = "http://www.jeuxvideo.com/forums/ajax_forum_prefere.php";
                                            String hash = Parser.hash3(html);
                                            String timestamp = Parser.timestamp(html);
                                            HashMap<String, String> values = new HashMap<String, String>();

                                            values.put("id_forum", Integer.toString(forum.getId()));
                                            values.put("id_topic", "0");
                                            values.put("action", "delete");
                                            values.put("type", "forum");
                                            values.put("ajax_timestamp", timestamp);
                                            values.put("ajax_hash", hash);

                                            Ajax.url(url).data(values).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                                    .ignoreContentType(true).callback(new AjaxCallback() {
                                                @Override
                                                public void onComplete(Connection.Response response) {
                                                    dialog.dismiss();
                                                    if (response != null) {
                                                        String a = json(response.body());
                                                        if (a == null) {
                                                            App.toast(R.string.favorite_deleted);
                                                            mAdapter.remove(position);
                                                            return;
                                                        }
                                                        App.alert(getActivity(), a);
                                                    } else {
                                                        App.alert(getActivity(), R.string.no_response);
                                                    }
                                                }
                                            }).execute();

                                        } else {
                                            dialog.dismiss();
                                            App.alert(getActivity(), R.string.no_response);
                                        }
                                    }
                                }).execute();
                    }

                    private void deleteTopic(final Topic topic, final int position) {
                        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
                        String url = Helper.topicToUrl(topic);

                        dialog.show();
                        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                if (response != null) {
                                    String url = App.HOST_WEB + "/forums/ajax_forum_prefere.php";
                                    String html = response.body();
                                    final HashMap<String, String> data = new HashMap<String, String>();

                                    data.put("ajax_timestamp", Parser.timestamp(html));
                                    data.put("ajax_hash", Parser.hash3(html));
                                    data.put("id_forum", Integer.toString(topic.getIdForum()));
                                    data.put("id_topic", Integer.toString(Parser.idTopic(html)));
                                    data.put("type", "topic");
                                    data.put("action", "delete");

                                    Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).ignoreContentType(true)
                                            .data(data).callback(new AjaxCallback() {
                                        @Override
                                        public void onComplete(Connection.Response response) {
                                            dialog.dismiss();
                                            if (response != null) {
                                                String a = json(response.body());
                                                if (a == null) {
                                                    App.toast(R.string.favorite_deleted);
                                                    mAdapter.remove(position);
                                                    return;
                                                }
                                                App.alert(getActivity(), a);
                                            } else {
                                                App.alert(getActivity(), R.string.no_response);
                                            }
                                        }


                                    }).execute();
                                } else {
                                    App.alert(getActivity(), R.string.no_response);
                                    dialog.dismiss();
                                }
                            }
                        }).execute();
                    }
                }));

        int title = R.string.subtitle_favoris_forums;
        if (mType == ListType.Topic) title = R.string.subtitle_favoris_topic;
        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources().getString(title));

        return layout;
    }

    public String json(String message) {
        try {
            JSONObject json = new JSONObject(message);
            JSONArray errors = json.getJSONArray("erreur");
            if (errors.length() != 0) {
                return errors.get(0).toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class FavoriteAdapter extends RecyclerViewAdapter<FavoriteHolder> {

        private ArrayList<Link> mValues;

        public FavoriteAdapter() {
            mValues = new ArrayList<Link>();
            load();
        }

        public FavoriteAdapter(ArrayList<Link> values) {
            mValues = new ArrayList<Link>(values);
            load();
        }

        private void load() {
            switch (mType) {
                case Forum:
                    load(0);
                    break;
                case Topic:
                    load(1);
            }
        }

        public ArrayList<Link> getData() {
            return mValues;
        }

        @Override
        public FavoriteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_link, parent, false);
            return new FavoriteHolder(view);
        }

        @Override
        public void onBindViewHolder(FavoriteHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        private void load(final int what) {
            String cookie =  Auth.getInstance().getCookieValue();
            Ajax.url(URL).cookie(Auth.COOKIE_NAME, cookie)
                    .callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            if (response != null) {
                                try {
                                    mValues = Parser.listFavorite(response.parse(), what);
                                    notifyDataSetChanged();

                                    return;
                                } catch (NoContentFoundException e) {
                                    App.snack(getView(), e.getMessage());
                                } catch (IOException e) {
                                    App.alert(getActivity(), e.getMessage());
                                }
                            } else {
                                App.snack(getView(), R.string.no_response);
                            }
                            if (mRecycler != null) mRecycler.showNoResults();
                        }
                    }).execute();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }

        public void remove(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
            if (mValues.size() == 0) mRecycler.showNoResults();
        }
    }

    private class FavoriteHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;

        public FavoriteHolder(View itemView) {
            super(itemView);

            mTitle = (TextView) itemView.findViewById(R.id.text_link);
        }

        public void bind(final Link link) {
            mTitle.setText(link.getContent());
        }
    }

}
