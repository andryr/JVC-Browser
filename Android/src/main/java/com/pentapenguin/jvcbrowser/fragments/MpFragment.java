package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.ForumActivity;
import com.pentapenguin.jvcbrowser.MpActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.TopicActivity;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Bans;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Post;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.NormalizePost;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerToolbarTranslation;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MpFragment extends Fragment {

    public static final String TAG = "mp";
    public static final String MP_ARG = "arg_mp";

    private RecyclerView2 mRecycler;
    private TitleObserver mListener;
    private MpAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private Topic mTopic;
    private int mOffset;
    private boolean mLocked;
    private MpNewFragment mPost;
    private int mToolbarHeight;

    public static MpFragment newInstance(Topic topic) {
        MpFragment fragment = new MpFragment();
        Bundle args = new Bundle();

        args.putParcelable(MP_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (TitleObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTopic = getArguments().getParcelable(MP_ARG);
        mAdapter = new MpAdapter();
        mListener.updateTitle(mTopic.getContent());

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_mp, container, false);

        mSwipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.mp_refresh_layout);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.mp_post_list);
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            mToolbarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        mSwipeLayout.setProgressViewOffset(false, mToolbarHeight, 150);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setEmptyView(layout.findViewById(R.id.mp_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.mp_loading_bar));

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLocked = true;
        mOffset = 0;
        mPost = (MpNewFragment) getChildFragmentManager().findFragmentByTag(MpNewFragment.TAG);

        mPost.setTopic(mTopic);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSwipeLayout.setColorSchemeColors(Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mAdapter.load();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = ((MpActivity) getActivity()).getToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mRecycler.addOnScrollListener(new RecyclerToolbarTranslation(toolbar, mToolbarHeight));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 777) {
            mAdapter.load();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_mp, menu);
        if (mLocked) menu.findItem(R.id.menu_mp_lock).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_mp_lock:
                if (!mLocked) {
                    App.alertOkCancel(getActivity(), "Voulez vous vraiment locker ce MP ?",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    lock();
                                }
                            });
                }
                return true;
            case R.id.menu_mp_refresh:
                reload();
                return true;
        }
        return false;
    }

    public void reload() {
        mOffset = 0;
        mAdapter.load();
    }

    private void lock() {
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        final String url = Helper.mpToUrl(mTopic, mOffset);

        dialog.show();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                .callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        if (response != null) {
                            try {
                                Document doc = response.parse();
                                HashMap<String, String> data = Parser.hidden(doc, "action-right");
                                Ajax.url(url).data(data).post().cookie(Auth.COOKIE_NAME,
                                        Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                                    @Override
                                    public void onComplete(Connection.Response response) {
                                        dialog.dismiss();
                                        if (response != null) {
                                            mAdapter.load();

                                            return;
                                        }
                                        App.alert(getActivity(), R.string.no_response);
                                    }
                                }).execute();
                            } catch (IOException e) {
                                App.alert(getActivity(), e.getMessage());
                            }

                            return ;
                        }
                        dialog.dismiss();
                        App.alert(getActivity(), R.string.no_response);
                    }
                }).execute();
    }

    private class MpAdapter extends RecyclerView.Adapter<MpHolder> {

        private ArrayList<Post> mValues;
        private LayoutInflater mInflater;

        public MpAdapter() {
            mValues = new ArrayList<Post>();
            mInflater = getActivity().getLayoutInflater();
            load();
        }

        public void load() {
            String url = Helper.mpToUrl(mTopic, mOffset);

            if (mSwipeLayout != null && !mSwipeLayout.isRefreshing()) {
                mValues.clear();
                notifyDataSetChanged();
            }
            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            mValues = Parser.mp(doc);
                            Iterator<Post> it = mValues.iterator();

                            while (it.hasNext()) {
                                Post post = it.next();
                                if (Bans.isBanned(post.getAuthor())) it.remove();
                            }
                            if (mValues.size() == 0) throw new NoContentFoundException();
                            mOffset = Parser.mpOffset(doc);
                            mLocked = Parser.hidden(doc, "form-post-topic").size() == 0;
                            if (mLocked) getChildFragmentManager().beginTransaction().hide(mPost).commit();
                            if (getActivity() != null) getActivity().supportInvalidateOptionsMenu();
                            mListener.updateTitle(Parser.getTitleMp(doc));
                            notifyDataSetChanged();
                            if (mOffset == 0) mRecycler.scrollToPosition(getItemCount());

                            return;
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        } catch (NoContentFoundException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                        mRecycler.showNoResults();

                        return;
                    }
                    App.alert(getActivity(), R.string.no_response);
                }
            }).execute();
        }

        @Override
        public MpHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_post, parent, false);
            return new MpHolder(view);
        }

        @Override
        public void onBindViewHolder(MpHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    private class MpHolder extends RecyclerView.ViewHolder {

        private ImageView mThumb;
        private TextView mAuthor;
        private TextView mDate;
        private TextView mControl;
        private WebView mContent;

        public MpHolder(View view) {
            super(view);
            mThumb = (ImageView) view.findViewById(R.id.post_thumb_image);
            mAuthor = (TextView) view.findViewById(R.id.post_author_text);
            mDate = (TextView) view.findViewById(R.id.post_date_text);
            mControl = (TextView) view.findViewById(R.id.post_control);
            mContent = (WebView) view.findViewById(R.id.post_content);

            mContent.getSettings().setDefaultTextEncodingName("utf-8");
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

        public void bind(final Post post) {
            mAuthor.setText(post.getAuthor());
            mDate.setText(post.getDate());
            Picasso.with(getActivity()).load(post.getProfilThumb()).into(mThumb);
            String content = NormalizePost.parse(post.getHtml().clone()).text();
            mContent.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            if (Auth.getInstance().getPseudo().toLowerCase().equals(post.getAuthor().toLowerCase())) {
                mControl.setVisibility(View.GONE);
            } else {
                mControl.setVisibility(View.VISIBLE);
            }
            mControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu(post);
                }
            });
        }

        private void popupMenu(final Post post) {
            PopupMenu menu = new PopupMenu(getActivity(), mControl);

            menu.getMenu().add("Ignorer");
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getTitle().equals("Ignorer")) {
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
    }
}
