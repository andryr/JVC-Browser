package com.pentapenguin.jvcbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.ForumFragment;
import com.pentapenguin.jvcbrowser.fragments.TopicFragment;

public class ForumActivity extends AppCompatActivity implements ForumFragment.ForumObserver {

    public static final String TITLE_SAVE = "title";

    private Toolbar mToolbar;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        if (savedInstanceState == null) {
            Forum forum = getIntent().getParcelableExtra(ForumFragment.FORUM_ARG);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, ForumFragment.newInstance(forum), ForumFragment.TAG);
            transaction.commit();
        } else {
            mToolbar.setSubtitle(savedInstanceState.getString(TITLE_SAVE));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE_SAVE, mTitle);

    }

    @Override
    public void gotoTopic(Topic topic) {
        Intent intent = new Intent(this, TopicActivity.class);
        intent.putExtra(TopicFragment.TOPIC_ARG, topic);
        startActivity(intent);
    }

    @Override
    public void updateTitle(String title) {
        mTitle = title;
        if (mToolbar != null) mToolbar.setSubtitle(title);
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }
}
