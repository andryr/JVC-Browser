package com.pentapenguin.jvcbrowser.app.navigation;

import android.content.Intent;
import android.support.v4.app.Fragment;
import com.pentapenguin.jvcbrowser.AuthActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.SettingsActivity;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.fragments.*;

public class NotConnectedNavigationMenu {

    public static final int LENGTH = 11;

    public static final NavigationFragment.NavigationType[] types = new NavigationFragment.NavigationType[] {
            NavigationFragment.NavigationType.Header,          // header
            NavigationFragment.NavigationType.Item,            // se connecter
            NavigationFragment.NavigationType.Category,        // forum
            NavigationFragment.NavigationType.Item,            // tous les forums
            NavigationFragment.NavigationType.Item,            // recherche les forums
            NavigationFragment.NavigationType.Item,            // historique
            NavigationFragment.NavigationType.Category,        // paramètres
            NavigationFragment.NavigationType.Item,            // options
            NavigationFragment.NavigationType.Item,            // bannis
            NavigationFragment.NavigationType.Category,        // about
            NavigationFragment.NavigationType.Item             // contributeurs
    };

    public static final String[] contents = App.getContext().getResources().getStringArray(R.array.not_connected);

    public static final int[] thumbs = new int[] {
            0,
            R.drawable.menu_authentification,
            0,
            R.drawable.menu_all_forums,
            0,
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
            ForumListFragment.newInstance(),
            ForumSearchFragment.newInstance(),
            HistoryFragment.newInstance(),
            null,
            null,
            BannedFragment.newInstance(),
            null,
            null
    };

    public static final Intent[] intents = new Intent[] {
            null,
            new Intent(App.getContext(), AuthActivity.class),
            null,
            null,
            null,
            null,
            null,
            new Intent(App.getContext(), SettingsActivity.class),
            null,
            null,
            null
    };
}
