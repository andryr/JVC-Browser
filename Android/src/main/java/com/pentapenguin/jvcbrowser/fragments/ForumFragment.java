package com.pentapenguin.jvcbrowser.fragments;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.*;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.*;
import android.widget.*;
import com.pentapenguin.jvcbrowser.TopicNewActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.*;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.widgets.*;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
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

public class ForumFragment extends Fragment {

    public static final String TAG = "forum";
    public static final String FORUM_ARG = "forum_arg";
    public static final String SEARCH_CHOICE_SAVE = "search_choice";
    public static final String SEARCH_SAVE = "search";
    public static final String CURRENT_PAGE_SAVE = "current_page";
    public static final String DATA_SAVE = "themes";
    public static final String TITLE_SAVE = "title";

    private Button mNewTopic;
    private Forum mForum;
    private ForumAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private RecyclerView2 mRecycler;
    private SearchView mSearchView;
    private int mSearchChoice;
    private String mSearch;
    private int mCurrentPage;
    private boolean mLoaded;
    private String mTitle;

    public static ForumFragment newInstance(Forum forum) {
        ForumFragment fragment = new ForumFragment();
        Bundle args = new Bundle();

        args.putParcelable(FORUM_ARG, forum);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mForum = getArguments().getParcelable(FORUM_ARG);
        mCurrentPage = 1;
        mSearch = "";
        mSearchChoice = -1;

        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(CURRENT_PAGE_SAVE);
            mSearch = savedInstanceState.getString(SEARCH_SAVE);
            mSearchChoice = savedInstanceState.getInt(SEARCH_CHOICE_SAVE);
            mTitle = savedInstanceState.getString(TITLE_SAVE);
            ArrayList<Topic> data = savedInstanceState.getParcelableArrayList(DATA_SAVE);
            mAdapter = new ForumAdapter(data);
        } else {
            mAdapter = new ForumAdapter();
        }
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.forumFragment, container, false);
        mNewTopic = (Button) layout.findViewById(R.id.forum_new_topic_button);
        mSwipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.forum_refresh_layout);
        mRecycler = (RecyclerView2) layout.findViewById(R.id.forum_topic_list);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setEmptyView(layout.findViewById(R.id.forum_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.forum_loading_bar));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        if (item != null) {
                            ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance((Topic) item), true);
                            History.add((Topic) item);
                        }
                    }

                    @Override
                    public void onLongClick(Object item, int position) {
                        if (item != null) {
                            Topic topic = (Topic) item;
                            ((FragmentLauncher) getActivity()).launch(TopicFragment
                                    .newInstance(topic.page(topic.getPostsNumber()/20 + 1)), true);
                            History.add((Topic) item);
                        }
                    }
                }));

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (Auth.getInstance().isConnected()) {
            mNewTopic.setVisibility(View.VISIBLE);
            mRecycler.addOnScrollListener(new RecyclerFloatingButtonListener(mNewTopic));
        }
        if (mTitle != null) ((TitleObserver) getActivity()).updateTitle(mTitle);
        mNewTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), TopicNewActivity.class);
                intent.putExtra(TopicNewFragment.TOPIC_NEW_ARG, mForum);
                startActivityForResult(intent, 666);
            }
        });
        mSwipeLayout.setColorSchemeColors(Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSearchChoice = -1;
                mCurrentPage = 1;
                mAdapter.load();
            }
        });
        mLoaded = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SEARCH_SAVE, mSearch);
        outState.putInt(SEARCH_CHOICE_SAVE, mSearchChoice);
        outState.putInt(CURRENT_PAGE_SAVE, mCurrentPage);
        outState.putString(TITLE_SAVE, mTitle);
        outState.putParcelableArrayList(DATA_SAVE, mAdapter.mValues);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == TopicNewActivity.RESULT_CODE) {
            Topic topic = data.getParcelableExtra(TopicPageFragment.TOPIC_ARG);
            ((FragmentLauncher) getActivity()).launch(TopicFragment.newInstance(topic), true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_forum, menu);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_forum_search));
        mSearchView.setQueryHint("Recherche");
        mSearchView.setBackgroundResource(R.color.white);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearch = query;
                searchDialog();
                return false;
            }

            private void searchDialog() {
                mSearchChoice = 0;
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.app_name)
                        .setPositiveButton("Chercher", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                App.hideKeyboard(mSearchView.getWindowToken());
                                search();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                App.hideKeyboard(mSearchView.getWindowToken());
                                dialogInterface.dismiss();
                            }
                        })
                        .setSingleChoiceItems(new String[]{"Sujet", "Pseudo"}, 0,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mSearchChoice = i;
                                    }
                                })
                        .create().show();
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        menu.findItem(R.id.menu_forum_previous).setVisible(mCurrentPage != 1);
        menu.findItem(R.id.menu_forum_favorite).setVisible(Auth.getInstance().isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_forum_goto_page:
                gotoPage();
                return true;
            case R.id.menu_forum_previous:
                mCurrentPage--;
                mAdapter.load();
                return true;
            case R.id.menu_forum_next:
                mCurrentPage++;
                mAdapter.load();
                return true;
            case R.id.menu_forum_favorite:
                addFavorite();
                break;
        }
        return false;
    }

    private void addFavorite() {
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

        dialog.show();
        String url = Helper.forumToUrl(mForum);
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

                            values.put("id_forum", Integer.toString(mForum.getId()));
                            values.put("id_topic", "0");
                            values.put("action", "add");
                            values.put("type", "forum");
                            values.put("ajax_timestamp", timestamp);
                            values.put("ajax_hash", hash);

                            Ajax.url(url).data(values).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                    .ignoreContentType(true).callback(new AjaxCallback() {
                                @Override
                                public void onComplete(Connection.Response response) {
                                    dialog.dismiss();
                                    if (response != null) {
                                        String a = response.body();
                                        String js = json(a);
                                        if (js != null) {
                                            App.alert(getActivity(), js);
                                        } else {
                                            App.toast(R.string.favorite_added);
                                        }
                                    } else {
                                        App.alert(getActivity(), R.string.no_response);
                                    }
                                }

                                private String json(String input) {
                                    try {
                                        JSONObject json = new JSONObject(input);
                                        JSONArray errors = json.getJSONArray("erreur");
                                        if (errors.length() != 0) return errors.getString(0);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            }).execute();

                        } else {
                            dialog.dismiss();
                            App.alert(getActivity(), R.string.no_response);
                        }
                    }
                }).execute();
    }

    private void gotoPage() {
        final EditText pageNumber = new EditText(getActivity());
        pageNumber.setHint(R.string.page_number);
        pageNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(getActivity())
                .setView(pageNumber)
                .setTitle(R.string.app_name)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (pageNumber.getText().toString().equals("")) {
                            Toast.makeText(getActivity(), R.string.field_empty, Toast.LENGTH_LONG).show();
                            return;
                        }
                        mCurrentPage = Math.abs(Integer.parseInt(pageNumber.getText().toString()));
                        mAdapter.load();
                        App.hideKeyboard(pageNumber.getWindowToken());
                    }
                })
                .setNegativeButton("Fermer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        App.hideKeyboard(pageNumber.getWindowToken());
                        dialogInterface.dismiss();
                    }
                })
                .create().show();
    }

    private void search() {
        mSwipeLayout.setRefreshing(true);
        mAdapter.load();
    }

    private class ForumAdapter extends RecyclerViewAdapter<ForumHolder> {

        private ArrayList<Topic> mValues;

        public ForumAdapter() {
            mValues = new ArrayList<Topic>();
            load();
        }

        public ForumAdapter(ArrayList<Topic> values) {
            mValues = values;
        }

        public Topic itemAt(int position) {
            try {
                return mValues.get(position);
            } catch (ArrayIndexOutOfBoundsException ignored) { }

            return null;
        }

        public void load() {
            Forum forum = new Forum(mForum.getId(), mCurrentPage, "");
            String url = mSearchChoice == -1 ? Helper.forumToMobileUrl(forum) :
                    Helper.forumToSearchMobileUrl(forum, mSearchChoice, mSearch);

            Ajax.url(url).post().callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    if (mSwipeLayout.isRefreshing()) mSwipeLayout.setRefreshing(false);
                    if (response != null) {
                        try {
                            Document doc = response.parse();
                            mValues = Parser.forum(doc);
                            Iterator<Topic> it = mValues.iterator();

                            while (it.hasNext()) {
                                Topic topic = it.next();
                                if (Bans.isBanned(topic.getAuthor())) it.remove();
                            }
                            if (mValues.size() == 0) throw new NoContentFoundException();
                            mTitle = (mCurrentPage == 1 ? "" : mCurrentPage + " | ") + Parser.getTitleForum(doc);
                            ((TitleObserver) getActivity()).updateTitle(mTitle);
                            if (getActivity() != null) getActivity().supportInvalidateOptionsMenu();
                            notifyDataSetChanged();
                            mLoaded = true;

                            return;
                        } catch (NoContentFoundException e) {
                            App.snack(getView(), e.getMessage());
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
        public ForumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(Theme.topic, parent, false);
            return new ForumHolder(view);
        }

        @Override
        public void onBindViewHolder(ForumHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    private class ForumHolder extends RecyclerView2.ViewHolder {

        private TextView mContent;
        private TextView mAuthor;
        private TextView mPostsNumber;
        private TextView mDate;
        private ImageView mThumb;

        public ForumHolder(View itemView) {
            super(itemView);

            mContent = (TextView) itemView.findViewById(R.id.forum_item_content);
            mAuthor = (TextView) itemView.findViewById(R.id.forum_item_author);
            mPostsNumber = (TextView) itemView.findViewById(R.id.forum_item_posts_number);
            mDate = (TextView) itemView.findViewById(R.id.forum_item_date);
            mThumb = (ImageView) itemView.findViewById(R.id.forum_item_thumb);
        }

        public void bind(Topic topic) {
            mContent.setText(topic.getContent());
            mAuthor.setText(topic.getAuthor());
            mPostsNumber.setText("(" + Integer.toString(topic.getPostsNumber()) + ")");
            mDate.setText(topic.getLastPostDate());
            Picasso.with(getActivity()).load(topic.getThumbUrl()).into(mThumb);
        }
    }
}
