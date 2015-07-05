package com.pentapenguin.jvcbrowser.app.navigation;

import android.content.Intent;
import android.support.v4.app.Fragment;
import com.pentapenguin.jvcbrowser.AuthActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.fragments.*;

public class NotConnectedNavigationMenu {

    public static final int LENGTH = 11;

    public static final NavigationFragment.NavigationType[] types = new NavigationFragment.NavigationType[] {
            NavigationFragment.NavigationType.Header,          //0 header
            NavigationFragment.NavigationType.Category,        //1 authentification
            NavigationFragment.NavigationType.Item,            //2 se connecter
            NavigationFragment.NavigationType.Category,        //3 forum
            NavigationFragment.NavigationType.Item,            //4 tous les forums
            NavigationFragment.NavigationType.Item,            //5 historique
            NavigationFragment.NavigationType.Category,        //6 paramètres
            NavigationFragment.NavigationType.Item,            //7 options
            NavigationFragment.NavigationType.Item,            //8 bannis
            NavigationFragment.NavigationType.Category,        //8 about
            NavigationFragment.NavigationType.Item             //10 contributeurs
    };

    public static final String[] contents = App.getContext().getResources().getStringArray(R.array.not_connected);

    public static final int[] thumbs = new int[] {
            0,
            0,
            R.drawable.menu_authentification,
            0,
            R.drawable.menu_all_forums,
            R.drawable.menu_history,
            0,
            R.drawable.menu_options,
            R.drawable.menu_banned,
            0,
            R.drawable.menu_contributers
    };

    public static final Fragment[] fragments = new Fragment[] {
            null,
            null,
            null,
            null,
            ForumListFragment.newInstance(),
            HistoryFragment.newInstance(),
            null,
            null,
            BannedFragment.newInstance(),
            null,
            null
    };

    public static final Intent[] intents = new Intent[] {
            null,
            null,
            new Intent(App.getContext(), AuthActivity.class),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    };
}
