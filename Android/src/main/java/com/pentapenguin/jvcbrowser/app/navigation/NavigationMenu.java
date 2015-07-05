package com.pentapenguin.jvcbrowser.app.navigation;


import com.pentapenguin.jvcbrowser.entities.Navigation;

import java.util.ArrayList;

public class NavigationMenu {

    public enum Type {Connected, NotConnected}

    public static ArrayList<Navigation> create(Type type) {
        switch (type) {
            case Connected:
                return connected();
            case NotConnected:
                return notConnected();
        }

        return null;
    }

    private static ArrayList<Navigation> notConnected() {
        ArrayList<Navigation> navs = new ArrayList<Navigation>();
        for (int i = 0; i < NotConnectedNavigationMenu.LENGTH; i++) {

            navs.add(new Navigation(
                    NotConnectedNavigationMenu.types[i],
                    NotConnectedNavigationMenu.fragments[i],
                    NotConnectedNavigationMenu.intents[i],
                    NotConnectedNavigationMenu.contents[i],
                    NotConnectedNavigationMenu.thumbs[i],
                    ""));
        }

        return navs;
    }

    private static ArrayList<Navigation> connected() {
        ArrayList<Navigation> navs = new ArrayList<Navigation>();
        for (int i = 0; i < ConnectedNavigationMenu.LENGTH; i++) {

            navs.add(new Navigation(
                    ConnectedNavigationMenu.types[i],
                    ConnectedNavigationMenu.fragments[i],
                    ConnectedNavigationMenu.intents[i],
                    ConnectedNavigationMenu.contents[i],
                    ConnectedNavigationMenu.thumbs[i],
                    ""));
        }

        return navs;
    }

}
