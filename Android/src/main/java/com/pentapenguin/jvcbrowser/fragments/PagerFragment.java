package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.*;
import android.widget.EditText;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.util.HashMap;

public class PagerFragment extends Fragment {

    public static final String PAGER_ARG = "pager_arg";
    public static final String TAG = "pager";

    private ViewPager mPager;
    private Topic mTopic;
    private PagerAdapter mAdapter;
    private PagerObserver mListener;
    private PostNewFragment mPost;

    public static PagerFragment newInstance(Topic topic) {
        PagerFragment fragment = new PagerFragment();
        Bundle args = new Bundle();

        args.putParcelable(PAGER_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (PagerObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopic = getArguments().getParcelable(PAGER_ARG);
        mAdapter = new PagerAdapter(getFragmentManager());

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_pager, container, false);
        mPager = (ViewPager) layout.findViewById(R.id.topic_pager);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPost = (PostNewFragment) getChildFragmentManager().findFragmentByTag(PostNewFragment.TAG);

        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) { }

            @Override
            public void onPageSelected(int i) {
                mListener.updateTitle(i + 1);
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });
        if (!Auth.getInstance().isConnected()) {
            getChildFragmentManager().beginTransaction().hide(mPost).commit();
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
                mPager.setCurrentItem(mAdapter.getCount(), true);
                return true;
            case R.id.menu_topic_goto_page:
                gotoPage();
                return true;
            case R.id.menu_topic_refresh:
                mListener.reloadPage();
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

    public void updatePages(int max) {
        mAdapter.update(max);
    }

    public void updatePostUrl(String postUrl) {
        mPost.setPostUrl(postUrl);
    }

    public void gotoLastItem() {
        mPager.setCurrentItem(mAdapter.getCount(), true);
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

    public void appendPost(Topic topic) {
        mPost.append(topic.getContent());
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        private int mMax;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            mMax = 1;
        }

        public void update(int max) {
            if (mMax != max) {
                mMax = max;
                notifyDataSetChanged();
            }
        }

        @Override
        public Fragment getItem(int i) {
            return TopicFragment.newInstance(mTopic.page(i + 1));
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

    public interface PagerObserver {
        void reloadPage();
        void updateTitle(int page);
    }
}