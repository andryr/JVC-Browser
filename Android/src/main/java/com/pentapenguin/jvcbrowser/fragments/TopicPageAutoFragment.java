package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.EditActivity;
import com.pentapenguin.jvcbrowser.ProfileActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.*;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Post;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;
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

public class TopicPageAutoFragment extends Fragment {

    public static final String TAG = "topic";
    public static final String TOPIC_ARG = "topic_arg";
    private static final int ITEM_CACHED_VIEW = 20;
    private static final int EXTRA_LAYOUT_SPACE = 10000;
    private static final int WAIT_BEFORE_SCROLL = 1000;
    private static final String DATA_SAVE = "data";
    private static final String LOADED_SAVE = "loaded";
    private static final long REFRESH_TIME = 10000;

    private RecyclerView2 mRecycler;
    private TopicAdapter mAdapter;
    private LinearLayoutManager mLayout;
    private Topic mTopic;
    private boolean mLoaded;
    private boolean mRefreshing;

    public static TopicPageAutoFragment newInstance(Topic topic) {
        TopicPageAutoFragment fragment = new TopicPageAutoFragment();
        Bundle args = new Bundle();

        args.putParcelable(TOPIC_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopic = getArguments().getParcelable(TOPIC_ARG);
        mLoaded = false;
        mRefreshing = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);

        if (savedInstanceState != null) {
            mLoaded = savedInstanceState.getBoolean(LOADED_SAVE);
            ArrayList<Post> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mAdapter = mRefreshing ? new TopicAutoAdapter(data) : new TopicAdapter(data);
        } else {
            mAdapter = mRefreshing ? new TopicAutoAdapter() : new TopicAdapter();
        }
        mAdapter.load();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_topic_page, container, false);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.topic_post_list);
        mRecycler.setAdapter(mAdapter);
        mLayout = new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return EXTRA_LAYOUT_SPACE;
            }
        };
        mRecycler.setEmptyView(layout.findViewById(R.id.topic_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.topic_loading_bar));
        mRecycler.setItemViewCacheSize(ITEM_CACHED_VIEW);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.setLayoutManager(mLayout);
        mRecycler.setHasFixedSize(true);

        return layout;
    }

//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//        mRefreshing = isVisibleToUser ? Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false) : false;
//        if (mRefreshing && mAdapter != null) {
//            mAdapter.load();
//            return;
//        }
//        if (isVisibleToUser && !mLoaded && mAdapter != null) {
//            mAdapter.load();
//        }
//    }

    @Override
    public void onPause() {
        super.onPause();
        mRefreshing = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRefreshing = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);
        if (mRefreshing) mAdapter.load();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOADED_SAVE, mLoaded);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.mValues);
    }

    public void reload() {
        if (!mRefreshing) mAdapter.load();
    }

    public void scrollToBottom() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecycler.smoothScrollToPosition(mAdapter.getItemCount());
            }
        }, WAIT_BEFORE_SCROLL);
    }

    private class TopicAdapter extends RecyclerView2.Adapter<RecyclerView.ViewHolder> {

        protected ArrayList<Post> mValues;

        public TopicAdapter() {
            mValues = new ArrayList<Post>();
        }

        public TopicAdapter(ArrayList<Post> values) {
            mValues = new ArrayList<Post>(values);
        }

        public void load() {

            final boolean shouldScrollDown = mLoaded && mLayout != null &&
                    mLayout.findLastVisibleItemPosition() >= mAdapter.getItemCount() - 3;

            Ajax.url(Helper.topicToMobileUrl(mTopic) + "?bide=" + System.currentTimeMillis()).post()
                    .callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            if (response != null) {
                                try {
                                    Document doc = response.parse();
                                    ArrayList<Post> values = Parser.topic(doc);
                                    Iterator<Post> it = values.iterator();

                                    while (it.hasNext()) {
                                        Post post = it.next();
                                        if (Bans.isBanned(post.getAuthor())) it.remove();
                                    }
                                    int max = values.size();
                                    if (max == 0) throw new NoContentFoundException();
                                    int pages = Parser.page(doc);
                                    ((TopicObserver) getParentFragment()).updatePages(pages);
                                    if (mValues.size() >= max + 1) return;
                                    if (mValues.isEmpty()) mValues.add(0, new Post(pages, Parser.getTitleTopic(doc)));
                                    for (int i = mValues.size(); i < max + 1; i++) {
                                        mValues.add(values.get(i - 1));
                                        notifyItemInserted(i);
                                    }
                                    ((TopicObserver) getParentFragment()).updatePostUrl(Parser.newPostUrl(doc));
                                    if (shouldScrollDown) scrollToBottom();
                                    mLoaded = true;
                                    mRecycler.hideEmpties();

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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TopicFragment.TopicType type = TopicFragment.TopicType.values()[viewType];

            if (type == TopicFragment.TopicType.Title) {
                View view = getActivity().getLayoutInflater().inflate(R.layout.item_topic_header, parent, false);
                return new TopicHeaderHolder(view);
            }
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_post, parent, false);
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
            return position == 0 ? TopicFragment.TopicType.Title.ordinal() : TopicFragment.TopicType.Core.ordinal();
        }
    }

    private class TopicAutoAdapter extends TopicAdapter {

        public TopicAutoAdapter() {
            super();
        }

        public TopicAutoAdapter(ArrayList<Post> values) {
            super(values);
        }

        @Override
        public void load() {
            if (!mRefreshing) return;
            final boolean shouldScrollDown = mLoaded && mLayout != null &&
                    mLayout.findLastVisibleItemPosition() >= mAdapter.getItemCount() - 3;

            Ajax.url(Helper.topicToMobileUrl(mTopic) + "?bide=" + System.currentTimeMillis()).post()
                    .callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    load();
                                }
                            }, REFRESH_TIME);

                            if (response != null) {
                                try {
                                    Document doc = response.parse();
                                    ArrayList<Post> values = Parser.topic(doc);
                                    Iterator<Post> it = values.iterator();

                                    while (it.hasNext()) {
                                        Post post = it.next();
                                        if (Bans.isBanned(post.getAuthor())) it.remove();
                                    }
                                    int max = values.size();
                                    if (max == 0) throw new NoContentFoundException();
                                    int pages = Parser.page(doc);
                                    ((TopicObserver) getParentFragment()).updatePages(pages);
                                    if (mValues.size() >= max + 1) return;
                                    if (mValues.isEmpty()) mValues.add(0, new Post(pages, Parser.getTitleTopic(doc)));
                                    for (int i = mValues.size(); i < max + 1; i++) {
                                        mValues.add(values.get(i - 1));
                                        notifyItemInserted(i);
                                    }
                                    ((TopicObserver) getParentFragment()).updatePostUrl(Parser.newPostUrl(doc));
                                    if (shouldScrollDown) scrollToBottom();
                                    mLoaded = true;
                                    mRecycler.hideEmpties();

                                    return;
                                } catch (IOException e) {
//                                    App.alert(getActivity(), e.getMessage());
                                } catch (NoContentFoundException e) {
//                                    App.alert(getActivity(), e.getMessage());
                                }
                            } else {
//                                App.alert(getActivity(), R.string.no_response);
                            }
                            mRecycler.showNoResults();
                        }
                    }).execute();
        }
    }

    private class TopicHolder extends RecyclerView.ViewHolder {

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

        private void edit(Post post) {
            Intent intent = new Intent(getActivity(), EditActivity.class);
            intent.putExtra(EditFragment.EDIT_TOPIC_ARG, mTopic);
            intent.putExtra(EditFragment.POST_ID_ARG, post.getId());
            getParentFragment().startActivityForResult(intent, 777);
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
            mTitle.setText("Sujet : " + post.getContent() + "\nPage " + mTopic.getPage() + "/" + post.getId());
        }
    }

    public interface TopicObserver {
        void updatePages(int max);
        void updatePostUrl(String postUrl);
        void gotoLastPage();
        void quote(Topic topic);
    }

}