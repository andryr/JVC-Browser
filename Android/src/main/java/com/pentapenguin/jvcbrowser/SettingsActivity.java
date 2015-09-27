package com.pentapenguin.jvcbrowser;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Settings;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;

public class SettingsActivity extends PreferenceActivity {

//    private Boolean mTopicAuto;
//    private Boolean mMpAuto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
//        Toast.makeText(this, "L'autorefresh desactivay mouahaha !", Toast.LENGTH_LONG).show();

//        final Preference topicAutoPreference = findPreference(Settings.TOPIC_AUTOREFRESH);
//        final Preference mpAutoPreference = findPreference(Settings.TOPIC_AUTOREFRESH);
//        mTopicAuto = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);
//        mMpAuto = Storage.getInstance().get(Settings.TOPIC_AUTOREFRESH, false);
//
//        topicAutoPreference.setDefaultValue(mTopicAuto);
//        topicAutoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                mTopicAuto = !mTopicAuto;
//                Storage.getInstance().put(Settings.TOPIC_AUTOREFRESH, mTopicAuto);
//                return false;
//            }
//        });
//        mpAutoPreference.setDefaultValue(mTopicAuto);
//        mpAutoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                mMpAuto = !mMpAuto;
//                Storage.getInstance().put(Settings.MP_AUTOREFRESH, mMpAuto);
//                return false;
//            }
//        });
        final ListPreference themePreference = (ListPreference) findPreference("theme_settings");

        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Storage.getInstance().put(Settings.THEME, Integer.parseInt(newValue.toString()) - 1);
                Toast.makeText(SettingsActivity.this,
                        "Vous devez supprimer l'application de la liste des taches puis la relancer !"
                       , Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

}
