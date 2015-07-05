package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.MpFragment;
import com.pentapenguin.jvcbrowser.fragments.MpNewFragment;
import com.pentapenguin.jvcbrowser.util.TitleObserver;

public class MpActivity extends AppCompatActivity implements TitleObserver, MpNewFragment.MpNewObserver {

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            Topic topic = getIntent().getParcelableExtra(MpFragment.MP_ARG);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, MpFragment.newInstance(topic), MpFragment.TAG);
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void updateTitle(String title) {
        if (mToolbar != null) mToolbar.setSubtitle(title);
    }

    @Override
    public void onPost() {
        MpFragment fragment = (MpFragment) getSupportFragmentManager().findFragmentByTag(MpFragment.TAG);
        fragment.reload();
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }
}
