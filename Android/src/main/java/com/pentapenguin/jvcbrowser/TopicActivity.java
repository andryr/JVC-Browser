package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.PagerFragment;
import com.pentapenguin.jvcbrowser.fragments.PostNewFragment;
import com.pentapenguin.jvcbrowser.fragments.TopicFragment;

import java.util.List;

public class TopicActivity extends AppCompatActivity implements TopicFragment.TopicObserver,
        PagerFragment.PagerObserver, PostNewFragment.PostNewObserver {

    private Toolbar mToolbar;
    private PagerFragment mFragment;
    private int mPage = 1;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        Topic topic = getIntent().getParcelableExtra(TopicFragment.TOPIC_ARG);
        mFragment = PagerFragment.newInstance(topic);

        setSupportActionBar(mToolbar);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_main, mFragment, PagerFragment.TAG);
        transaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void gotoLastPage() {
        FragmentManager manager = getSupportFragmentManager();
        PagerFragment fragment = (PagerFragment) manager.findFragmentByTag(PagerFragment.TAG);
        fragment.gotoLastItem();
    }

    public void reloadPage() {
        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment instanceof TopicFragment && fragment.getUserVisibleHint()) {
                ((TopicFragment) fragment).reload();
            }
        }
    }

    @Override
    public void updatePages(int max) {
        mFragment.updatePages(max);
    }

    @Override
    public void updatePostUrl(String postUrl) {
        mFragment.updatePostUrl(postUrl);
    }

    @Override
    public void quote(Topic topic) {
        mFragment.appendPost(topic);
    }

    @Override
    public void updateTitle(String title) {
        mTitle = title;
        updateTitle();
    }

    @Override
    public void updateTitle(int page) {
        mPage = page;
        updateTitle();
    }

    private void updateTitle() {
        mToolbar.setSubtitle(mPage + " | " + mTitle);
    }

    @Override
    public void onPost(Topic topic) {
        gotoLastPage();
        reloadPage();
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }
}