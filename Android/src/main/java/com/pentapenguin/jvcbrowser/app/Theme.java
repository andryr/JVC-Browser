package com.pentapenguin.jvcbrowser.app;

import android.util.Log;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;

public class Theme {

    public static int toolbar;

    public static int authActivity;
    public static int fragmentActivity;
    public static int mainActivity;

    public static int bannedFragment;
    public static int editFragment;
    public static int favoriteFragment;
    public static int forumFragment;
    public static int forumListFragment;
    public static int forumSearchFragment;
    public static int historyFragment;
    public static int inboxFragment;
    public static int inboxNewFragment;
    public static int mpFragment;
    public static int mpNewFragment;
    public static int navigationFragment;
    public static int notificationFragment;
    public static int postNewFragment;
    public static int profileFragment;
    public static int subscribeFragment;
    public static int topicFragment;
    public static int topicNewFragment;
    public static int topicPageFragment;

    public static int link;
    public static int mp;
    public static int post;
    public static int topic;
    public static int topicHeader;
    public static int forumListChild;
    public static int forumListGroup;

    public static int navigationHeader;
    public static int navigationCategory;
    public static int navigationItem;

    static {
        Field[] fields = Theme.class.getDeclaredFields();
        try {
            String theme = App.getResourceAsset(Assets.themes[Storage.getInstance().get(Settings.THEME, 1)]);
            JSONObject json = new JSONObject(theme);
            for (Field field : fields) {
                field.setInt(field, App.getRessourceId(R.layout.class, json.getString(field.getName())));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        for (Field field : fields) {
            try {
                Log.d(field.getName(), field.get(field).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
