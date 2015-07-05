package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.EditFragment;

public class EditActivity extends AppCompatActivity implements EditFragment.EditObserver {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setSubtitle(R.string.subtitle_edit);
        setSupportActionBar(toolbar);
        if (savedInstanceState == null) {
            Topic topic = getIntent().getParcelableExtra(EditFragment.EDIT_TOPIC_ARG);

            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.frame_main, EditFragment.newInstance(topic), EditFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onPost() {
        setResult(667);
        finish();
    }
}
