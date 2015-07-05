package com.pentapenguin.jvcbrowser.util.network;

import org.jsoup.Connection;

public interface AjaxCallback {

    void onComplete(Connection.Response response);
}
