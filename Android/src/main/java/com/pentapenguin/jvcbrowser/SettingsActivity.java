package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.pentapenguin.jvcbrowser.app.Settings;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;

public class SettingsActivity extends PreferenceActivity {


    private Boolean mTopicAuto;
    private Boolean mMpAuto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final Preference topicAutoPreference = findPreference(Settings.TOPIC_AUTOREFRESH);
        final Preference mpAutoPreference = findPreference(Settings.TOPIC_AUTOREFRESH);
        mTopicAuto = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);
        mMpAuto = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);

        topicAutoPreference.setDefaultValue(mTopicAuto);
        topicAutoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mTopicAuto = !mTopicAuto;
                Storage.getInstance().put(Settings.TOPIC_AUTOREFRESH, mTopicAuto);
                return false;
            }
        });
        mpAutoPreference.setDefaultValue(mTopicAuto);
        mpAutoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mMpAuto = !mMpAuto;
                Storage.getInstance().put(Settings.MP_AUTOREFRESH, mMpAuto);
                return false;
            }
        });
    }
}
