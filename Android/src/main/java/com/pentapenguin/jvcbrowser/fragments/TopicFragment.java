package com.pentapenguin.jvcbrowser.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.*;
import android.widget.EditText;
import com.pentapenguin.jvcbrowser.EditActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.ItemPosted;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.util.HashMap;

public class TopicFragment extends Fragment implements TopicPageFragment.TopicObserver, ItemPosted {

    public static final String PAGER_ARG = "pager_arg";
    public static final String TAG = "pager";
    public static final String CURRENT_PAGE_SAVE = "current_page";
    public static final String MAX_SAVE = "max";
    public static final String TITLE_SAVE = "title";

    private ViewPager mPager;
    private Topic mTopic;
    private PagerAdapter mAdapter;
    private PostNewFragment mPost;
    private int mMax;
    private int mCurrentPage;
    private String mTitle;

    public static TopicFragment newInstance(Topic topic) {
        TopicFragment fragment = new TopicFragment();
        Bundle args = new Bundle();

        args.putParcelable(PAGER_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopic = getArguments().getParcelable(PAGER_ARG);
        mMax = 1;
        mCurrentPage = 0;
        mTitle = null;

        if (savedInstanceState != null) {
            mMax = savedInstanceState.getInt(MAX_SAVE);
            mCurrentPage = savedInstanceState.getInt(CURRENT_PAGE_SAVE);
            mTitle = savedInstanceState.getString(TITLE_SAVE);
            mPost = (PostNewFragment) getChildFragmentManager().findFragmentByTag(PostNewFragment.TAG);
        } else {
            mPost = PostNewFragment.createInstance();
            getChildFragmentManager().beginTransaction().add(R.id.pager_post_new_layout, mPost, PostNewFragment.TAG)
                    .commit();
        }
        mAdapter = new PagerAdapter(getChildFragmentManager());
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_topic, container, false);
        mPager = (ViewPager) layout.findViewById(R.id.topic_pager);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(1);
        if (mCurrentPage != 0) mPager.setCurrentItem(mCurrentPage);
        if (!Auth.getInstance().isConnected()) {
            getChildFragmentManager().beginTransaction().hide(mPost).commit();
        }
        if (mTitle != null) {
            ((TitleObserver) getActivity()).updateTitle((mCurrentPage + 1) + " | " + mTitle);
        }
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mCurrentPage = i;
                ((TitleObserver) getActivity()).updateTitle((mCurrentPage + 1) + " | " + mTitle);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_PAGE_SAVE, mPager.getCurrentItem());
        outState.putInt(MAX_SAVE, mMax);
        outState.putString(TITLE_SAVE, mTitle);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == EditActivity.RESULT_CODE) {
            reloadCurrentPage();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_topic, menu);
        menu.findItem(R.id.menu_topic_favorite).setVisible(Auth.getInstance().isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pager_last:
                gotoLastPage();
                return true;
            case R.id.menu_topic_goto_page:
                gotoPage();
                return true;
            case R.id.menu_topic_favorite:
                addFavorite();
                return true;
        }
        return false;
    }

    private void addFavorite() {
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        String url = Helper.topicToUrl(mTopic);

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
                    data.put("id_forum", Integer.toString(mTopic.getIdForum()));
                    data.put("id_topic", Integer.toString(Parser.idTopic(html)));
                    data.put("type", "topic");
                    data.put("action", "add");

                    Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).ignoreContentType(true)
                            .data(data).callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            dialog.dismiss();
                            if (response != null) {
                                String a = json(response.body());
                                if (a == null) {
                                    App.toast(R.string.favorite_added);
                                    return;
                                }
                                App.alert(getActivity(), a);
                            } else {
                                App.alert(getActivity(), R.string.no_response);
                            }
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
                } else {
                    App.alert(getActivity(), R.string.no_response);
                    dialog.dismiss();
                }
            }
        }).execute();
    }

    private void gotoPage() {
        final EditText pageNumber = new EditText(getActivity());
        pageNumber.setHint("Numero de la page");
        pageNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(getActivity())
                .setView(pageNumber)
                .setTitle(R.string.app_name)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (pageNumber.getText().toString().equals("")) {
                            App.toast(R.string.field_empty);
                            return;
                        }
                        int page = Integer.parseInt(pageNumber.getText().toString());
                        mPager.setCurrentItem(page - 1);
                    }
                })
                .setNegativeButton("Fermer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create().show();
    }

    private void reloadCurrentPage() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment != null && fragment instanceof TopicPageFragment && fragment.isVisible()) {
                ((TopicPageFragment) fragment).reload();
            }
        }
    }

    @Override
    public void updatePostUrl(String postUrl) {
        if (mPost != null) mPost.setPostUrl(postUrl);
    }

    @Override
    public void gotoLastPage() {
        mPager.setCurrentItem(mAdapter.getCount(), true);
    }

    @Override
    public void updatePages(int max) {
        mAdapter.update(max);
    }

    @Override
    public void quote(Topic topic) {
        if (mPost != null) mPost.append(topic.getContent());
    }

    @Override
    public void updateTitle(String title) {
        if (mTitle == null) {
            mTitle = title;
            ((TitleObserver) getActivity()).updateTitle(title);
        }
    }

    @Override
    public void onPost(Item item) {
        gotoLastPage();
        reloadCurrentPage();
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void update(int max) {
            if (mMax != max) {
                mMax = max;
                notifyDataSetChanged();
            }
        }

        @Override
        public Fragment getItem(int i) {
            return TopicPageFragment.newInstance(mTopic.page(i + 1));
        }

        @Override
        public int getCount() {
            return mMax;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (position + 1) + "";
        }
    }

}