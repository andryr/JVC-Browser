package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.util.TypedValue;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.*;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Bans;
import com.pentapenguin.jvcbrowser.app.History;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Post;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerToolbarTranslation;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopicFragment extends Fragment {

    public static final String TAG = "topic";
    public static final String TOPIC_ARG = "topic_arg";
    public static final int ITEM_CACHED_VIEW = 20;
    public static final int EXTRA_LAYOUT_SPACE = 10000;
    public static final int WAIT_BEFORE_SCROLL = 1000;

    private RecyclerView2 mRecycler;
    private TopicAdapter mAdapter;
    private Topic mTopic;
    private TopicObserver mListener;
    private boolean mLoaded;
    private android.support.v7.widget.Toolbar mToolbar;
    private int mToolbarHeight;

    public static TopicFragment newInstance(Topic topic) {
        TopicFragment fragment = new TopicFragment();
        Bundle args = new Bundle();

        args.putParcelable(TOPIC_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (TopicObserver) activity;
            mToolbar = ((TopicActivity) activity).getToolbar();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopic = getArguments().getParcelable(TOPIC_ARG);
        mAdapter = new TopicAdapter();
        mLoaded = false;

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_topic, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.topic_post_list);
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            mToolbarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        mRecycler.setAdapter(mAdapter);
        mRecycler.setEmptyView(layout.findViewById(R.id.topic_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.topic_loading_bar));
        mRecycler.setItemViewCacheSize(ITEM_CACHED_VIEW);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(android.support.v7.widget.RecyclerView.State state) {
                return EXTRA_LAYOUT_SPACE;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            mRecycler.addOnScrollListener(new RecyclerToolbarTranslation(mToolbar, mToolbarHeight));
        }

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoaded = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !mLoaded && mAdapter != null) {
            mAdapter.load();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 667) reload();
    }

    public void reload() {
        mAdapter.load();
    }

    private class TopicAdapter extends RecyclerView2.Adapter<TopicHolder> {

        private ArrayList<Post> mValues;
        private LayoutInflater mInflater;

        public TopicAdapter() {
            mValues = new ArrayList<Post>();
            mInflater = getActivity().getLayoutInflater();
        }

        public void load() {
            mValues.clear();
            notifyDataSetChanged();
            Ajax.url(Helper.topicToMobileUrl(mTopic)).post().callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            mValues = Parser.topic(doc);
                            Iterator<Post> it = mValues.iterator();

                            while (it.hasNext()) {
                                Post post = it.next();
                                if (Bans.isBanned(post.getAuthor())) it.remove();
                            }
                            int length = mValues.size();
                            if (length == 0) throw new NoContentFoundException();
                            mListener.updatePostUrl(Parser.newPostUrl(doc));
                            mListener.updatePages(Parser.page(doc));
                            mListener.updateTitle(Parser.getTitleTopic(doc));
                            notifyDataSetChanged();
                            if (mLoaded) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRecycler.smoothScrollToPosition(mAdapter.getItemCount());
                                    }
                                }, WAIT_BEFORE_SCROLL);
                            }
                            mLoaded = true;

                            return;
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        } catch (NoContentFoundException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                    } else {
                        App.alert(getActivity(), R.string.no_response);
                    }
                    mRecycler.showNoResults();
                }
            }).execute();
        }

        public void removeItem(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
        }

        @Override
        public TopicHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_post, parent, false);
            return new TopicHolder(view);
        }

        @Override
        public void onBindViewHolder(TopicHolder holder, int position) {
            holder.bind(mValues.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    private class TopicHolder extends RecyclerView2.ViewHolder {

        private ImageView mThumb;
        private TextView mAuthor;
        private TextView mDate;
        private TextView mControl;
        private WebView mContent;

        public TopicHolder(View view) {
            super(view);
            mThumb = (ImageView) view.findViewById(R.id.post_thumb_image);
            mAuthor = (TextView) view.findViewById(R.id.post_author_text);
            mDate = (TextView) view.findViewById(R.id.post_date_text);
            mControl = (TextView) view.findViewById(R.id.post_control);
            mContent = (WebView) view.findViewById(R.id.post_content);

            mControl.setVisibility(Auth.getInstance().isConnected() ? View.VISIBLE : View.GONE);
            mContent.getSettings().setDefaultTextEncodingName("utf-8");
            mContent.setBackgroundColor(Color.TRANSPARENT);
            mContent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return true;
                }
            });
            mContent.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    String pattern = "^http://(m|www)\\.jeuxvideo\\.com/forums/" +
                            "[0-9]*?-[0-9]*?-[0-9]*?-[0-9]*?-[0-9]*?-[0-9]*?-.*\\.htm$";
                    Matcher matcher = Pattern.compile(pattern).matcher(url);
                    if (matcher.matches()) {
                        Item item = Helper.urlResolve(url);
                        Intent intent;

                        if (item != null) {
                            if (item instanceof Forum) {
                                Forum forum = (Forum) item;
                                intent = new Intent(getActivity(), ForumActivity.class);
                                intent.putExtra(ForumFragment.FORUM_ARG, forum);
                            } else {
                                Topic topic = (Topic) item;
                                intent = new Intent(getActivity(), TopicActivity.class);
                                intent.putExtra(TopicFragment.TOPIC_ARG, topic);
                                History.add(topic);
                            }
                            startActivity(intent);
                            return true;
                        }
                    }
                    openLink(url);
                    return true;
                }

                public void openLink(String url) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }

        public void bind(final Post post, final int position) {
            mAuthor.setText(post.getAuthor());
            mAuthor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ProfileActivity.class);
                    intent.putExtra(ProfileFragment.PROFILE_ARG, post.getAuthor().toLowerCase());
                    startActivity(intent);
                }
            });
            mDate.setText(post.getDate());
            Picasso.with(getActivity()).load(post.getProfilThumb()).into(mThumb);
            String content = post.getContent();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                if (content.length() < 2000) {
//                    mContent.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//                }
//            }
            mContent.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            mControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu(post, position);
                }
            });
        }

        private void popupMenu(final Post post, final int position) {
            PopupMenu menu = new PopupMenu(getActivity(), mControl);

            menu.getMenu().add("Citer");
            if (Auth.getInstance().getPseudo().toLowerCase().equals(post.getAuthor().toLowerCase())) {
                menu.getMenu().add("Editer");
                menu.getMenu().add("Delete");

            } else {
                menu.getMenu().add("Ignorer");
            }
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getTitle().equals("Citer")) {
                        quote(post);
                    } else if (menuItem.getTitle().equals("Editer")) {
                        edit(post);
                    } else if (menuItem.getTitle().equals("Delete")) {
                        delete(post, position);
                    } else if (menuItem.getTitle().equals("Ignorer")) {
                        ignore(post);
                    }
                    return false;
                }
            });
            menu.show();
        }

        private void ignore(Post post) {
            Bans.add(post.getAuthor());
            mAdapter.load();
        }

        private void delete(final Post post, final int position) {
            String url = Helper.topicToMobileUrl(mTopic).replace("http://m", "http://www");
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        String url = "http://www.jeuxvideo.com/forums/modal_del_message.php";
                        String html = response.body();
                        final HashMap<String, String> data = new HashMap<String, String>();

                        data.put("ajax_timestamp", Parser.timestamp(html));
                        data.put("ajax_hash", Parser.hash2(html));
                        data.put("type", "delete");
                        data.put("tab_message[]", Integer.toString(post.getId()));
                        Ajax.url(url).data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                .ignoreContentType(true).post().callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                dialog.dismiss();
                                if (response != null) {
                                    String error = json(response.body());
                                    if (error == null) {
                                        mAdapter.removeItem(position);
                                        return;
                                    }
                                    App.alert(getActivity(), error);
                                    return;
                                }
                                App.alert(getActivity(), R.string.delete_unavailable);
                            }

                            private String json(String message) {
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

                        }).execute();

                        return;
                    }
                    dialog.dismiss();
                    App.alert(getActivity(), R.string.no_response);
                }
            }).execute();
        }

        private void edit(final Post post) {
            String url = Helper.topicToMobileUrl(mTopic).replace("http://m", "http://www");
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        String url = "http://www.jeuxvideo.com/forums/ajax_edit_message.php";
                        String html = response.body();
                        final HashMap<String, String> data = new HashMap<String, String>();

                        data.put("ajax_timestamp", Parser.timestamp(html));
                        data.put("ajax_hash", Parser.hash1(html));
                        data.put("action", "get");
                        data.put("id_message", Integer.toString(post.getId()));
                        Ajax.url(url).data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                .ignoreContentType(true).callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                dialog.dismiss();
                                if (response != null) {
                                    String edit = json(response.body());
                                    if (edit != null) {
                                        String hash = data.get("ajax_hash");
                                        String timestamp = data.get("ajax_timestamp");
                                        Intent intent = new Intent(getActivity(), EditActivity.class);
                                        Topic topic = new Topic(mTopic.getId(), mTopic.getCode(),
                                                mTopic.getIdForum(), edit, hash, timestamp, null, post.getId());
                                        intent.putExtra(EditFragment.EDIT_TOPIC_ARG, topic);
                                        startActivityForResult(intent, 777);

                                        return;
                                    }
                                    App.alert(getActivity(), R.string.edit_unavailable);

                                    return;
                                }
                                App.alert(getActivity(), R.string.no_response);
                            }

                            private String json(String message) {
                                try {
                                    JSONObject json = new JSONObject(message);
                                    JSONArray errors = json.getJSONArray("erreur");
                                    if (errors.length() != 0) {
                                        return null;
                                    }
                                    return json.getString("html");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }

                        }).execute();

                        return;
                    }
                    dialog.dismiss();
                    App.alert(getActivity(), R.string.no_response);
                }
            }).execute();
        }

        private void quote(final Post post) {
            String url = Helper.topicToMobileUrl(mTopic).replace("http://m", "http://www");
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (response != null) {
                        String url = "http://www.jeuxvideo.com/forums/ajax_citation.php";
                        String html = response.body();
                        final HashMap<String, String> data = new HashMap<String, String>();

                        data.put("ajax_timestamp", Parser.timestamp(html));
                        data.put("ajax_hash", Parser.hash1(html));
                        data.put("id_message", Integer.toString(post.getId()));
                        Ajax.url(url).data(data).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                .post().ignoreContentType(true).callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                dialog.dismiss();
                                if (response != null) {
                                    String quote = json(post, response.body());
                                    if (quote != null) {
                                        mListener.quote(new Topic(mTopic.getId(), quote));

                                        return;
                                    }
                                    App.alert(getActivity(), R.string.quote_unavailable);

                                    return;
                                }
                                App.alert(getActivity(), R.string.no_response);
                            }

                            private String json(Post post, String message) {
                                try {
                                    JSONObject json = new JSONObject(message);
                                    JSONArray errors = json.getJSONArray("erreur");
                                    if (errors.length() != 0) {
                                        return null;
                                    }
                                    String quote = json.getString("txt");
                                    quote = "> Le " + post.getDate() + " " + post.getAuthor() + " a " +
                                            getActivity().getResources().getString(R.string.write) +" :\n>"
                                            + quote.replaceAll("\\n", "\n> ") + "\n\n";
                                    return quote;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }).execute();

                        return;
                    }
                    dialog.dismiss();
                    App.alert(getActivity(), R.string.no_response);
                }
            }).execute();
        }
    }

    public interface TopicObserver extends TitleObserver {
        void updatePages(int max);
        void updatePostUrl(String postUrl);
        void quote(Topic topic);
    }

}