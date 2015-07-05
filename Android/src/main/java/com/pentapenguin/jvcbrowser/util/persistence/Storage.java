package com.pentapenguin.jvcbrowser.util.persistence;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.pentapenguin.jvcbrowser.app.App;

public class Storage {

    private static Storage storage;

    public static Storage getInstance() {
        if (storage == null)
            storage = new Storage();
        return storage;
    }

    public <T>  void put(String key, T value) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(App.getContext());
        SharedPreferences.Editor editor = preferences.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        }
        apply(editor);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(App.getContext());

        if (defaultValue instanceof String || defaultValue == null) {
            return (T) preferences.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return (T) ((Integer) preferences.getInt(key, (Integer) defaultValue));
        } else if (defaultValue instanceof Boolean) {
            return (T) ((Boolean) preferences.getBoolean(key, (Boolean) defaultValue));
        } else if (defaultValue instanceof Float) {
            return (T) ((Float) preferences.getFloat(key, (Float) defaultValue));
        }
        return null;
    }

    public void remove(String key) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(App.getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        apply(editor);
    }

    private void apply(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }
}
