package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.*;
import com.pentapenguin.jvcbrowser.entities.*;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.*;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
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

public class MpFragment extends Fragment implements ItemPosted {

    public static final String TAG = "mp";
    public static final String MP_ARG = "arg_mp";
    public static final String LOCKED_SAVE = "locked";
    public static final String OFFSET_SAVE = "offset";
    public static final String DATA_SAVE = "data";
    public static final String TITLE_SAVE = "title";
    private static final long WAIT_BEFORE_SCROLL = 1500;

    private RecyclerView2 mRecycler;
    private MpAdapter mAdapter;
    private SwipyRefreshLayout mSwipeLayout;
    private Mp mMp;
    private int mOffset;
    private boolean mLocked;
    private MpNewFragment mPost;
    private String mTitle;
    private LinearLayoutManager mLayout;
    private boolean mLoaded;

    public static MpFragment newInstance(Mp mp) {
        MpFragment fragment = new MpFragment();
        Bundle args = new Bundle();

        args.putParcelable(MP_ARG, mp);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocked = true;
        mOffset = 0;
        mTitle = null;
        mLoaded = false;
        mMp = getArguments().getParcelable(MP_ARG);
        if (savedInstanceState != null) {
            mLoaded = true;
            mLocked = savedInstanceState.getBoolean(LOCKED_SAVE);
            mOffset = savedInstanceState.getInt(OFFSET_SAVE);
            mTitle = savedInstanceState.getString(TITLE_SAVE);
            ArrayList<Post> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mPost = (MpNewFragment) getChildFragmentManager().findFragmentByTag(MpNewFragment.TAG);
            mAdapter = new MpAdapter(data);
        } else {
            mAdapter = new MpAdapter();
            mPost = MpNewFragment.createInstance();
            getChildFragmentManager().beginTransaction().add(R.id.mp_new_frame, mPost, MpNewFragment.TAG)
                    .commit();
        }
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_mp, container, false);

        mSwipeLayout = (SwipyRefreshLayout) layout.findViewById(R.id.mp_refresh_layout);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.mp_post_list);
        mLayout = new LinearLayoutManager(getActivity());
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(mLayout);
        mRecycler.setEmptyView(layout.findViewById(R.id.mp_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.mp_loading_bar));

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mPost!= null) mPost.setTopic(mMp);
        if (savedInstanceState == null) mAdapter.load(SwipyRefreshLayoutDirection.BOTTOM);
        if (mTitle != null) ((TitleObserver) getActivity()).updateTitle(mTitle);
        mSwipeLayout.setColorSchemeColors(Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN);
        mSwipeLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
                mAdapter.load(swipyRefreshLayoutDirection);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOCKED_SAVE, mLocked);
        outState.putString(TITLE_SAVE, mTitle);
        outState.putInt(OFFSET_SAVE, mOffset);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.mValues);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_mp, menu);
        menu.findItem(R.id.menu_mp_lock).setVisible(!mLocked);
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
        }
        return false;
    }

    private void lock() {
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        final String url = Helper.mpToUrl(mMp, mOffset);

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
                                            mAdapter.load(SwipyRefreshLayoutDirection.BOTTOM);

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

    @Override
    public void onPost(Item item) {
        mAdapter.load(SwipyRefreshLayoutDirection.BOTTOM);
        mAdapter.scrollToBottom();
    }

    private class MpAdapter extends RecyclerView.Adapter<MpHolder> {

        protected ArrayList<Post> mValues;

        public MpAdapter() {
            mValues = new ArrayList<Post>();
        }

        public MpAdapter(ArrayList<Post> values) {
            mValues = new ArrayList<Post>(values);
        }

        public void load1(final SwipyRefreshLayoutDirection direction) {
            String url = Helper.mpToUrl(mMp, mOffset);
            if (direction == SwipyRefreshLayoutDirection.TOP) mValues.clear();

            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            mOffset = Parser.mpOffset(doc);
                            if (!mLoaded) {
                                mLoaded = true;
                                load(SwipyRefreshLayoutDirection.BOTTOM);
                                return;
                            }
                            ArrayList<Post> values = Parser.mp(doc);
                            Iterator<Post> it = values.iterator();

                            while (it.hasNext()) {
                                Post post = it.next();
                                if (Bans.isBanned(post.getAuthor())) it.remove();
                            }
                            if (values.size() == 0) throw new NoContentFoundException();
                            mLocked = Parser.hidden(doc, "form-post-topic").size() == 0;
                            if (mLocked) getChildFragmentManager().beginTransaction().hide(mPost).commit();
                            mTitle = Parser.getTitleMp(doc);
                            ((TitleObserver) getActivity()).updateTitle(mTitle);
                            ((ServiceUpdate) getActivity()).mpUpdate(Parser.mpUnread(doc));
                            if (getActivity() != null) getActivity().supportInvalidateOptionsMenu();
                            int max = values.size();
                            if (mValues.size() >= max) return;
                            for (int i = mValues.size(); i < max; i++) {
                                mValues.add(values.get(i));
                                notifyItemInserted(i);
                            }
                            mRecycler.hideEmpties();
                            if (direction == SwipyRefreshLayoutDirection.BOTTOM) scrollToBottom();

                            return;
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        } catch (NoContentFoundException e) {
                            App.snack(getView(), e.getMessage());
                        }
                    } else {
                        App.snack(getView(), R.string.no_response);
                    }
                    mRecycler.showNoResults();
                }
            }).execute();
        }

        public void load(final SwipyRefreshLayoutDirection direction) {
            if (direction == SwipyRefreshLayoutDirection.BOTTOM) {
                mOffset = 0;
                mLayout.setStackFromEnd(true);
            } else {
                mLayout.setStackFromEnd(false);
            }
            String url = Helper.mpToUrl(mMp, mOffset);
            mValues.clear();

            Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            ArrayList<Post> values = Parser.mp(doc);
                            Iterator<Post> it = values.iterator();

                            while (it.hasNext()) {
                                Post post = it.next();
                                if (Bans.isBanned(post.getAuthor())) it.remove();
                            }
                            mOffset = Parser.mpOffset(doc);
                            if (values.size() == 0) throw new NoContentFoundException();
                            mLocked = Parser.hidden(doc, "form-post-topic").size() == 0;
                            if (mLocked) getChildFragmentManager().beginTransaction().hide(mPost).commit();
                            mTitle = Parser.getTitleMp(doc);
                            ((TitleObserver) getActivity()).updateTitle(mTitle);
                            ((ServiceUpdate) getActivity()).mpUpdate(Parser.mpUnread(doc));
                            if (getActivity() != null) getActivity().supportInvalidateOptionsMenu();
                            mValues.addAll(values);
                            notifyDataSetChanged();

                            return;
                        } catch (IOException e) {
                            App.alert(getActivity(), e.getMessage());
                        } catch (NoContentFoundException e) {
                            App.snack(getView(), e.getMessage());
                        }
                    } else {
                        App.snack(getView(), R.string.no_response);
                    }
                    mRecycler.showNoResults();
                }
            }).execute();

        }

        public void scrollToBottom() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecycler.smoothScrollToPosition(mAdapter.getItemCount());
                }
            }, WAIT_BEFORE_SCROLL);
        }

        @Override
        public MpHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_post, parent, false);
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
            mAdapter.load(SwipyRefreshLayoutDirection.BOTTOM);
        }
    }
}
