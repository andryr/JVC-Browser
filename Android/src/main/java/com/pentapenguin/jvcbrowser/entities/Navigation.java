package com.pentapenguin.jvcbrowser.entities;

import android.content.Intent;
import android.support.v4.app.Fragment;
import com.pentapenguin.jvcbrowser.fragments.NavigationFragment;

public class Navigation {

    private NavigationFragment.NavigationType type;
    private Fragment fragment = null;
    private Intent intent = null;
    private String content;
    private String details = "";
    private int thumb = 0;


    public Navigation(NavigationFragment.NavigationType type, Fragment fragment, Intent intent, String content,
                      int thumb, String details) {
        this.type = type;
        this.fragment = fragment;
        this.content = content;
        this.thumb = thumb;
        this.details = details;
        this.intent = intent;
    }

    public NavigationFragment.NavigationType getType() {
        return type;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public String getContent() {
        return content;
    }

    public int getThumb() {
        return thumb;
    }

    public String getDetails() {
        return details;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
