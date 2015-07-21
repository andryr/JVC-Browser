package com.pentapenguin.jvcbrowser.app;

import com.pentapenguin.jvcbrowser.util.persistence.Storage;

import java.util.ArrayList;
import java.util.Arrays;

public class Bans {

    public static final String BANNED = "banned";
    public static final String DELIMITER = "#";

    public static String get() {
        return Storage.getInstance().get(BANNED, null);
    }

    public static boolean isBanned(String pseudo) {
        return get() != null && getList().contains(pseudo.toLowerCase());
    }

    public static boolean add(String pseudo) {
        if (isBanned(pseudo)) return false;
        ArrayList<String> bannis = getList();

        bannis.add(pseudo.toLowerCase());
        StringBuilder sb = new StringBuilder();
        for (String banni : bannis) sb.append(banni).append(DELIMITER);

        Storage.getInstance().put(BANNED, sb.toString().substring(0, sb.length() - 1));

        return true;
    }

    public static boolean remove(String pseudo) {
        if (!isBanned(pseudo)) return false;
        ArrayList<String> bannis = getList();
        StringBuilder sb = new StringBuilder();

        for (String banni : bannis) {
            if (!banni.toLowerCase().equals(pseudo.toLowerCase())) sb.append(banni).append(DELIMITER);
        }

        if (sb.length() != 0) {
            Storage.getInstance().put(BANNED, sb.toString().substring(0, sb.length() - 1));
        } else {
            reset();
        }

        return true;
    }

    public static ArrayList<String> getList() {
        if (get() == null) return new ArrayList<String>();
        return new ArrayList<String>(Arrays.asList(get().split(DELIMITER)));
    }

    public static void reset() {
        Storage.getInstance().remove(BANNED);
    }
}
