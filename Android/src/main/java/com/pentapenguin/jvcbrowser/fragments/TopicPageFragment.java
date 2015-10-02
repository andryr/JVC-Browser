package com.pentapenguin.jvcbrowser.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.webkit.*;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.*;
import com.pentapenguin.jvcbrowser.app.*;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Post;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.*;
import com.pentapenguin.jvcbrowser.util.network.*;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.SwipeRefreshLayoutBottom;
import com.squareup.okhttp.*;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopicPageFragment extends Fragment {

    public static final String TAG = "topic";
    public static final String TOPIC_ARG = "topic_arg";
    private static final int ITEM_CACHED_VIEW = 20;
    private static final int EXTRA_LAYOUT_SPACE = 10000;
    private static final String DATA_SAVE = "themes";
    private static final String LOADED_SAVE = "loaded";

    public enum Type {
        FirstUpdate, Update, Post, Edit
    }

    private RecyclerView2 mRecycler;
    private TopicAdapter mAdapter;
    private LinearLayoutManager mLayout;
    private SwipeRefreshLayoutBottom mSwipeLayout;
    private Topic mTopic;
    private boolean mLoaded;
    final OkHttpClient mClient = new OkHttpClient();
    private int mEdit;

    public static TopicPageFragment newInstance(Topic topic) {
        TopicPageFragment fragment = new TopicPageFragment();
        Bundle args = new Bundle();

        args.putParcelable(TOPIC_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient.setConnectTimeout(3, TimeUnit.SECONDS);
        mTopic = getArguments().getParcelable(TOPIC_ARG);
        mLoaded = false;

        if (savedInstanceState != null) {
            mLoaded = savedInstanceState.getBoolean(LOADED_SAVE);
            ArrayList<Post> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mAdapter = data.size() == 0 ? new TopicAdapter() : new TopicAdapter(data);
        } else {
            mAdapter = new TopicAdapter();
        }
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.topicPageFragment, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.topic_post_list);
        mSwipeLayout = (SwipeRefreshLayoutBottom) layout.findViewById(R.id.topic_page_swipe);
        mRecycler.setAdapter(mAdapter);
        mLayout = new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(android.support.v7.widget.RecyclerView.State state) {
                return EXTRA_LAYOUT_SPACE;
            }
        };
        mRecycler.setEmptyView(layout.findViewById(R.id.topic_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.topic_loading_bar));
        mRecycler.setItemViewCacheSize(ITEM_CACHED_VIEW);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.setLayoutManager(mLayout);
        mRecycler.setHasFixedSize(true);
        mSwipeLayout.setColorSchemeColors(Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayoutBottom.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeLayout.setRefreshing(false);
                mAdapter.load(Type.Update);
            }
        });

        return layout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !mLoaded && mAdapter != null) {
            mAdapter.load(Type.FirstUpdate);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOADED_SAVE, mLoaded);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.mValues);
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.cancel(this);
    }

    public void reload(final Type type) {
        if (!mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(true);
        mLoaded = true;
        mAdapter.load(type);
    }

    public void scrollToBottom() {
        mLayout.setStackFromEnd(true);
    }

    private class TopicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        protected ArrayList<Post> mValues;
        private boolean mNoContent;

        public TopicAdapter() {
            mValues = new ArrayList<Post>();
        }

        public TopicAdapter(ArrayList<Post> values) {
            mValues = new ArrayList<Post>(values);
        }

        public void load(Type type) {
            loadData(type);
        }

        @SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface" })
        private void loadData(final Type type) {
            final String command = "javascript:window.android.onComplete(document.documentElement.innerHTML);";
            final WebView web = new WebView(App.getContext());
            final String url = Helper.topicToMobileUrl(mTopic);
            web.setWebChromeClient(new WebChromeClient());
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 " +
                    "Firefox/4.0");
            web.addJavascriptInterface(new Object() {

                public int mCurrent;
                public int mMax;
                public String mPostUrl;
                public int mPages;
                private String mTitle;

                @JavascriptInterface
                public void onComplete(final String result) {
                    try {
                        final Document doc = Jsoup.parse(result);
                        ArrayList<Post> values = Parser.topic(doc);
                        mTitle = Parser.getTitleTopic(doc);
                        mPostUrl = Parser.newPostUrl(doc);
                        mPages = Parser.page(doc);
                        Iterator<Post> it = values.iterator();
                        while (it.hasNext()) {
                            Post post = it.next();
                            if (Bans.isBanned(post.getAuthor())) it.remove();
                        }
                        mMax = values.size() + 2;
                        if (mMax == 2) throw new NoContentFoundException();
                        mCurrent = mValues.size();

                        mValues.clear();
                        mValues.add(new Post(mPages, mTitle));
                        mValues.addAll(values);
                        mValues.add(new Post(mPages, mTitle));

                        successUI();
                        mNoContent = false;
                    } catch (NoContentFoundException ignored) {
                        failUI();
                    }
                }

                private void edit() {
                    notifyItemChanged(mEdit);
                }

                private void post() {
                    update();
                }

                private void update() {
                    for (int i = mCurrent; i < mMax; i++) {
                        notifyItemInserted(i);
                    }
                }

                private void firstUpdate() {
                    notifyDataSetChanged();
                }

                private void failUI() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                            mRecycler.showNoResults();
                        }
                    });
                }

                private void successUI() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                            ((TopicObserver) getParentFragment()).updatePages(mPages);
                            ((TopicObserver) getParentFragment()).updatePostUrl(mPostUrl);
                            if (mLoaded && mMax > 3) scrollToBottom();
                            mLoaded = true;

                            switch (type) {
                                case FirstUpdate:
                                    firstUpdate();
                                    break;
                                case Update:
                                    update();
                                    break;
                                case Post:
                                    post();
                                    break;
                                case Edit:
                                    edit();
                                    break;
                            }
                        }
                    });
                }

            }, "android");
            web.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    mNoContent = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mNoContent) {
                                if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                                web.stopLoading();
                            }
                        }
                    }, 5000);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    web.loadUrl(command);
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String uurl) {
                    if (url.equals(uurl)) return super.shouldInterceptRequest(view, uurl);

                    return new WebResourceResponse(null, null, null);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                            if (mAdapter.getItemCount() == 0) {
                                mRecycler.showNoResults();
                            } else {
                                App.snack(getView(), R.string.no_response);
                            }
                        }
                    });

                    super.onReceivedError(view, errorCode, description, failingUrl);
                }
            });
            web.loadUrl(url);
        }

        public void removeItem(int position) {
            mValues.remove(position);
            notifyItemRemoved(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TopicFragment.TopicType type = TopicFragment.TopicType.values()[viewType];
            if (getActivity() == null) return null;

            if (type == TopicFragment.TopicType.Title) {
                View view = getActivity().getLayoutInflater().inflate(Theme.topicHeader, parent, false);
                return new TopicHeaderHolder(view);
            }
            View view = getActivity().getLayoutInflater().inflate(Theme.post, parent, false);
            return new TopicHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TopicHeaderHolder) {
                ((TopicHeaderHolder) holder).bind(mValues.get(0));
            } else {
                ((TopicHolder) holder).bind(mValues.get(position), position);
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 || position == getItemCount() - 1 ?
                    TopicFragment.TopicType.Title.ordinal() : TopicFragment.TopicType.Core.ordinal();
        }
    }

    private class TopicHolder extends RecyclerView.ViewHolder {

        private ImageView mThumb;
        private TextView mAuthor;
        private TextView mDate;
        private TextView mControl;
        private WebView mContent;
        private View mFrame;

        public TopicHolder(View view) {
            super(view);
            mThumb = (ImageView) view.findViewById(R.id.post_thumb_image);
            mAuthor = (TextView) view.findViewById(R.id.post_author_text);
            mDate = (TextView) view.findViewById(R.id.post_date_text);
            mControl = (TextView) view.findViewById(R.id.post_control);
            mContent = (WebView) view.findViewById(R.id.post_content);
            mFrame = view.findViewById(R.id.item_post);

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

                        if (item != null) {
                            if (item instanceof Forum) {
                                ((FragmentLauncher) getActivity()).launch(ForumFragment.newInstance((Forum) item), true);
                            } else {
                                ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance((Topic) item), true);
                                History.add((Topic) item);
                            }
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
                        edit(post, position);
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

        private void edit(Post post, int position) {
            Intent intent = new Intent(getActivity(), EditActivity.class);
            mEdit = position;
            intent.putExtra(EditFragment.EDIT_TOPIC_ARG, mTopic);
            intent.putExtra(EditFragment.POST_ID_ARG, post.getId());
            getParentFragment().startActivityForResult(intent, 777);
        }

        private void ignore(Post post) {
            Bans.add(post.getAuthor());
            reload(Type.FirstUpdate);
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
                                        ((TopicObserver) getParentFragment()).quote(new Topic(mTopic.getId(), quote));

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

    private class TopicHeaderHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;

        public TopicHeaderHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.topic_header_title);
        }

        public void bind(Post post) {
            mTitle.setText(post.getContent() + "\n" + mTopic.getPage() + "/" + post.getId());
        }
    }

    public interface TopicObserver {
        void updatePages(int max);
        void updatePostUrl(String postUrl);
        void gotoLastPage();
        void quote(Topic topic);
        void reload(Type type);
    }

}