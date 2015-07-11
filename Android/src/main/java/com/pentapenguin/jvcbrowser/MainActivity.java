package com.pentapenguin.jvcbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.fragments.*;
import com.pentapenguin.jvcbrowser.util.*;

public class MainActivity extends AppCompatActivity implements ActivityLauncher, FragmentLauncher, TitleObserver {

    private static final int REQUEST_CODE = 666;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private boolean mDoubleTap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        NavigationFragment navigation = (NavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        mDrawerLayout = navigation.setUp((DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        mToolbar.setSubtitle(Auth.getInstance().isConnected() ?
                R.string.subtitle_favoris_forums : R.string.subtitle_all_forum);
        mDoubleTap = false;

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = Auth.getInstance().isConnected() ? FavoriteFragment
                    .newInstance(FavoriteFragment.ListType.Forum) : ForumListFragment.newInstance();
            transaction.replace(R.id.frame_main, fragment, FavoriteFragment.TAG);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
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
    }

    @Override
    public void updateTitle(String title) {
        if (mToolbar != null) mToolbar.setSubtitle(title);
    }

    @Override
    public void launch(Fragment fragment, boolean isBackStacked) {
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, fragment);
            if (isBackStacked) transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void launch(Intent intent) {
        if (intent != null) startActivityForResult(intent, REQUEST_CODE);
    }
}
