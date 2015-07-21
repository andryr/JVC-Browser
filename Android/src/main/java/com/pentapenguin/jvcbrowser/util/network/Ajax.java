package com.pentapenguin.jvcbrowser.util.network;

import android.os.AsyncTask;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Ajax extends AsyncTask<Void, Void, Connection.Response> {

    private Connection connection;
    private AjaxCallback listener;

    private Ajax(String url) {
        connection = HttpConnection.connect(url);
        header("Cache-control", "no-cache");
        header("Cache-store", "no-store");
        header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/37.0.2049.0 Safari/537.36");
    }

    public static Ajax url(String url) {
        return new Ajax(url);
    }

    public Ajax data(Map<String, String> data) {
        connection.data(data);
        return this;
    }

    public Ajax method(Connection.Method method) {
        connection.method(method);
        return this;
    }

    public Ajax timeout(int millis) {
        connection.timeout(millis);
        return this;
    }

    public Ajax data(String name, String value) {
        connection.cookie(name, value);
        return this;
    }

    public Ajax data(Collection<Connection.KeyVal> data) {
        connection.data(data);
        return this;
    }

    public Ajax cookies(Map<String, String> cookies) {
        connection.cookies(cookies);
        return this;
    }

    public Ajax cookie(String name, String value) {
        connection.cookie(name, value);
        return this;
    }

    public Ajax ignoreContentType(boolean shouldIgnore) {
        connection.ignoreContentType(shouldIgnore);
        return this;
    }

    public Ajax followRedirects(boolean followRedirects) {
        connection.followRedirects(followRedirects);
        return this;
    }

    public Ajax callback(AjaxCallback callback) {
        listener = callback;
        return this;
    }

    public Ajax header(String name, String value) {
        connection.header(name, value);
        return this;
    }

    public Ajax post() {
        method(Connection.Method.POST);
        return this;
    }

    @Override
    protected Connection.Response doInBackground(Void... voids) {
        try {
            return connection.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Connection.Response response) {
        super.onPostExecute(response);
        if (listener != null) {
            listener.onComplete(response);
        }
    }
}
