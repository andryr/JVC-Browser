package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.EditFragment;
import com.pentapenguin.jvcbrowser.util.ItemPosted;

public class EditActivity extends AppCompatActivity implements ItemPosted {

    public static final int RESULT_CODE = 444;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setSubtitle(R.string.subtitle_edit);
        setSupportActionBar(toolbar);
        if (savedInstanceState == null) {
            Topic topic = getIntent().getParcelableExtra(EditFragment.EDIT_TOPIC_ARG);
            int postId = getIntent().getIntExtra(EditFragment.POST_ID_ARG, 0);

            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.frame_main, EditFragment.newInstance(topic, postId), EditFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onPost(Item item) {
        setResult(RESULT_CODE);
        finish();
    }
}
