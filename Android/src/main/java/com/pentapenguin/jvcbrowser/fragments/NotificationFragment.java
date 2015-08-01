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
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.ServiceUpdate;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationFragment extends Fragment{

    public static final String TAG = "notifications";
    public static final String URL = "http://www.jeuxvideo.com/abonnements/ajax/list_notification.php";

    private NotificationAdapter mAdapter;
    private String mTimestamp;
    private String mHash;
    private RecyclerView2 mRecycler;

    public static NotificationFragment newInstance() {
        return new NotificationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_link, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.links_list);

        mAdapter = new NotificationAdapter();
        mRecycler.setAdapter(mAdapter);
        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources()
                .getString(R.string.subtitle_notifications));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        TextView empty = (TextView) layout.findViewById(R.id.link_empty_text);
        empty.setText(R.string.no_notifications);
        mRecycler.setLoadingView(layout.findViewById(R.id.link_loading_bar));
        mRecycler.setEmptyView(empty);
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        if (item != null) {
                            openNotification((Topic) item);
                            mAdapter.remove(position);
                        }
                    }
                }));

        return layout;
    }

    private void openNotification(final Topic topic) {
        String url = "http://www.jeuxvideo.com/abonnements/ajax/set_notification_lu.php";
        HashMap<String, String> data = new HashMap<String, String>();
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

        dialog.show();
        data.put("ajax_timestamp", mTimestamp);
        data.put("ajax_hash", mHash);
        data.put("id_notification", Integer.toString(topic.getPostsNumber()));
        Ajax.url(url).data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                .ignoreContentType(true).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                dialog.dismiss();
                if (response != null) {
                    String result = json(response.body());
                    if (result == null) {
                        ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance(topic), true);
                        ((ServiceUpdate) getActivity()).notificationUpdate(mAdapter.getItemCount());
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
                    JSONArray error = json.getJSONArray("erreur");
                    if (error.length() != 0) return error.get(0).toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.load();
    }

    private class NotificationAdapter extends RecyclerViewAdapter<NotificationHolder> {

        private ArrayList<Topic> mValues;

        public NotificationAdapter() {
            mValues = new ArrayList<Topic>();
        }

        private void load() {
            String url = "http://www.jeuxvideo.com/profil/{?}?mode=notifications";
            url = url.replace("{?}", Auth.getInstance().getPseudo().toLowerCase()).trim()
                    .replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            String[] infos = Parser.subscribeData(doc).split("#");
                            HashMap<String, String> data = new HashMap<String, String>();
                            mTimestamp = infos[0];
                            mHash = infos[1];

                            data.put("ajax_timestamp", mTimestamp);
                            data.put("ajax_hash", mHash);
                            data.put("start", "0");
                            data.put("type_cibles", "topic");
                            Ajax.url(URL).data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                    .post().ignoreContentType(true).callback(new AjaxCallback() {
                                @Override
                                public void onComplete(Connection.Response response) {
                                    if (response != null) {
                                        String html = json(response.body());

                                        if (html != null) {
                                            Document doc = Jsoup.parse(html);
                                            mValues = Parser.subscribeNotifications(doc);
                                            ((ServiceUpdate) getActivity()).notificationUpdate(mAdapter.getItemCount());
                                            if (!mValues.isEmpty()) notifyDataSetChanged();

                                            return;
                                        }
                                    } else {
                                        App.snack(getView(), R.string.no_response);
                                    }
                                    mRecycler.showNoResults();
                                }

                                private String json(String message) {
                                    try {
                                        JSONObject json = new JSONObject(message);
                                        String html = json.getString("html");
                                        if (!html.equals("")) return html;
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            }).execute();

                            return;
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                    } else {
                        App.snack(getView(), R.string.no_response);
                    }
                    mRecycler.showNoResults();
                }
            }).execute();
        }

        @Override
        public Object itemAt(int position) {
            try {
                return mValues.get(position);
            } catch (ArrayIndexOutOfBoundsException ignored) { }

            return null;
        }

        @Override
        public NotificationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_mp, parent, false);
            return new NotificationHolder(view);
        }

        @Override
        public void onBindViewHolder(NotificationHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void remove(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
        }
    }

    private class NotificationHolder extends RecyclerView.ViewHolder {

        private TextView mContent;
        private TextView mDate;
        private TextView mPostsNumber;

        public NotificationHolder(View view) {
            super(view);
            mContent = (TextView) view.findViewById(R.id.mp_item_content);
            mDate = (TextView) view.findViewById(R.id.mp_item_date);
            mPostsNumber = (TextView) view.findViewById(R.id.mp_item_author);
            view.findViewById(R.id.mp_item_lu).setVisibility(View.GONE);
        }

        public void bind(Topic topic) {
            mContent.setText(topic.getContent());
            mDate.setText(topic.getLastPostDate());
            Matcher matcher = Pattern.compile("^(.*) au sujet :$").matcher(topic.getAuthor().trim());
            if (matcher.find()) {
                mPostsNumber.setText(matcher.group(1));
            } else {
                mPostsNumber.setText(topic.getAuthor());
            }
        }
    }
}
