package com.pentapenguin.jvcbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.TopicPageFragment;
import com.pentapenguin.jvcbrowser.fragments.TopicNewFragment;
import com.pentapenguin.jvcbrowser.util.ItemPosted;

public class TopicNewActivity extends AppCompatActivity implements ItemPosted {

    public static final int RESULT_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toolbar.setSubtitle(R.string.subtitle_topic_new);
        if (savedInstanceState == null) {
            Forum forum = getIntent().getParcelableExtra(TopicNewFragment.TOPIC_NEW_ARG);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, TopicNewFragment.newInstance(forum), TopicNewFragment.TAG);
            transaction.commit();
        }
    }

    @Override
    public void onPost(Item topic) {
        Intent intent = new Intent();
        intent.putExtra(TopicPageFragment.TOPIC_ARG, (Topic) topic);
        setResult(RESULT_CODE, intent);
        finish();
    }
}
