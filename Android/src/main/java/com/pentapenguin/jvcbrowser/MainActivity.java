package com.pentapenguin.jvcbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Mp;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.fragments.*;
import com.pentapenguin.jvcbrowser.util.ItemObserver;
import com.pentapenguin.jvcbrowser.util.TitleObserver;

public class MainActivity extends AppCompatActivity implements ItemObserver, NavigationFragment.NavigationObserver,
        TitleObserver {

    public static final String TITLE_SAVE = "title";

    private Toolbar mToolbar;
    private NavigationFragment mNavigation;
    private DrawerLayout mDrawerLayout;
    private boolean mDoubleTap;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mNavigation = (NavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        mDrawerLayout = mNavigation.setUp((DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        mToolbar.setSubtitle(Auth.getInstance().isConnected() ?
                R.string.subtitle_favoris_forums : R.string.subtitle_all_forum);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = Auth.getInstance().isConnected() ? FavoriteFragment
                    .newInstance(FavoriteFragment.ListType.Forum) : ForumListFragment.newInstance();
            transaction.replace(R.id.frame_main, fragment, FavoriteFragment.TAG);
            transaction.commit();
        } else {
            mToolbar.setSubtitle(savedInstanceState.getString(TITLE_SAVE));
        }

        mDoubleTap = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE_SAVE, mTitle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 777) {
            mNavigation.reload();
        }
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (mDoubleTap) {
                super.onBackPressed();
                return;
            }
            mDoubleTap = true;
            App.toast(R.string.double_tap);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDoubleTap = false;
                }
            }, 3000);
        }
    }

    @Override
    public void updateTitle(String title) {
        mTitle = title;
        if (mToolbar != null) {
            mToolbar.setSubtitle(title);
        }
    }

    @Override
    public void gotoItem(Item item) {
        if (item == null) {
            return;
        }
        Intent intent = null;

        if (item instanceof Mp) {
            intent = new Intent(this, MpActivity.class);
            intent.putExtra(MpFragment.MP_ARG, (Topic) item);
        } else if (item instanceof Topic) {
            intent = new Intent(this, TopicActivity.class);
            intent.putExtra(TopicFragment.TOPIC_ARG, (Topic) item);
        } else if (item instanceof Forum) {
            intent = new Intent(this, ForumActivity.class);
            intent.putExtra(ForumFragment.FORUM_ARG, (Forum) item);
        }
        startActivity(intent);
    }

    @Override
    public void launchFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, fragment);
            transaction.commit();
        }
    }

    @Override
    public void launchActivity(Intent intent) {
        if (intent != null) startActivityForResult(intent, 666);
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }
}
