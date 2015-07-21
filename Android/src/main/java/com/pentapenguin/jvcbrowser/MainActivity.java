package com.pentapenguin.jvcbrowser;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.fragments.*;
import com.pentapenguin.jvcbrowser.services.UpdateService;
import com.pentapenguin.jvcbrowser.util.*;

public class MainActivity extends AppCompatActivity implements ActivityLauncher, FragmentLauncher, TitleObserver,
        ServiceUpdate {

    private static final int REQUEST_CODE = 666;
//    public static final long ALARM_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final long ALARM_INTERVAL = 1000*10;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private boolean mDoubleTap;
    private NavigationFragment mNavigation;
    private BrowserReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mNavigation = (NavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        mDrawerLayout = mNavigation.setUp((DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        mReceiver = new BrowserReceiver();
        mDoubleTap = false;

        if (getIntent().getBooleanExtra(UpdateService.MP_ACTION, false)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, InboxFragment.newInstance())
                    .commit();
        } else if (getIntent().getBooleanExtra(UpdateService.NOTIFICATION_ACTION, false)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, NotificationFragment.newInstance())
                    .commit();
        } else {
            if (savedInstanceState == null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = Auth.getInstance().isConnected() ? FavoriteFragment
                        .newInstance(FavoriteFragment.ListType.Forum) : ForumListFragment.newInstance();
                transaction.replace(R.id.frame_main, fragment);
                transaction.commit();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (getIntent().getBooleanExtra(UpdateService.MP_ACTION, false)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, InboxFragment.newInstance())
                    .commit();
        } else if (getIntent().getBooleanExtra(UpdateService.NOTIFICATION_ACTION, false)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, NotificationFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(UpdateService.MP_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AuthActivity.RESULT_CODE) {
            mNavigation.reload();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, UpdateService.class), 0);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                    ALARM_INTERVAL, pendingIntent);
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
        if (mToolbar != null) {
            mToolbar.setSubtitle(title);
        }
    }

    @Override
    public void launch(Fragment fragment, boolean isBackStacked) {
        if (fragment != null) {
            mToolbar.setSubtitle("");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, fragment);
//            transaction.setCustomAnimations(R.animator.slide_up, R.animator.slide_down);
            if (isBackStacked) transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void launch(Intent intent) {
        if (intent != null) startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void mpUpdate(int mp) {
        mNavigation.updateMp(mp);
    }

    @Override
    public void notificationUpdate(int notificationCount) {
        mNavigation.updateNotifications(notificationCount);
    }

    class BrowserReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UpdateService.MP_ACTION)) {
                int mps = intent.getIntExtra(UpdateService.MP_ARG, 0);
                mpUpdate(mps);
            }

        }
    }
}
