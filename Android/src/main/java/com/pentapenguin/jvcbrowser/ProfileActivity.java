package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.pentapenguin.jvcbrowser.fragments.ProfileFragment;

public class ProfileActivity extends AppCompatActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        findViewById(R.id.toolbar).setVisibility(View.GONE);
        if (savedInstanceState == null) {
            String pseudo = getIntent().getStringExtra(ProfileFragment.PROFILE_ARG);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, ProfileFragment.newInstance(pseudo))
                    .commit();
        }
    }
}
