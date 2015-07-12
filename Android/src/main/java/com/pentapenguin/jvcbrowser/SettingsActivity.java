package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;

public class SettingsActivity extends PreferenceActivity {

    public static final String AUTOREFRESH = "autorefresh";
    private Boolean mAutorefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final Preference autorefreshPreference = findPreference(AUTOREFRESH);
        mAutorefresh = Storage.getInstance().get(AUTOREFRESH, false);

        autorefreshPreference.setDefaultValue(mAutorefresh);
        autorefreshPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mAutorefresh = !mAutorefresh;
                Storage.getInstance().put(AUTOREFRESH, mAutorefresh);
                return false;
            }
        });
    }
}
