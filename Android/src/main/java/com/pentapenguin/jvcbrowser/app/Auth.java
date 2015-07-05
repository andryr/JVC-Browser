package com.pentapenguin.jvcbrowser.app;

import com.pentapenguin.jvcbrowser.util.persistence.Storage;

public class Auth {

    public static final String COOKIE_NAME = "coniunctio";
    public static final String PSEUDO = "pseudo";

    private String mPseudo;
    private String mCookie;
    private boolean mConnected;
    private static Auth sInstance;

    private Auth() {
        mConnected = false;
        mCookie = Storage.getInstance().get(COOKIE_NAME, null);
        if (mCookie != null) {
            mConnected = true;
            mPseudo = Storage.getInstance().get(PSEUDO, null);
        }
    }

    public static Auth getInstance() {
        if (sInstance == null) {
            sInstance = new Auth();
        }
        return sInstance;
    }

    public void connect(String pseudo, String cookie) {
        Storage.getInstance().put(Auth.COOKIE_NAME, cookie);
        Storage.getInstance().put(Auth.PSEUDO, pseudo);
        sInstance = new Auth();
    }

    public void disconnect() {
        Storage.getInstance().remove(COOKIE_NAME);
        Storage.getInstance().remove(PSEUDO);
        mPseudo = mCookie = null;
        mConnected = false;
    }

    public String getCookieName() {
        return COOKIE_NAME;
    }

    public String getCookieValue() {
        return mCookie;
    }

    public String getPseudo() {
        return mPseudo;
    }

    public boolean isConnected() {
        return mConnected;
    }

}
