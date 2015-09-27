package com.pentapenguin.jvcbrowser.util.network;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AjaxRaw extends AsyncTask<Void, Void, String> {

    private String mUrl;
    private AjaxRawCallback mListener;

    public AjaxRaw(String url, AjaxRawCallback callback) {
        mUrl = url;
        mListener = callback;
    }

    private String getHttp() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(mUrl).openConnection();
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-control", "max-age=0, no-cache, must-revalidate, proxy-revalidate, private");
        connection.setRequestProperty("Cache-store", "no-store");
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            String result = getHttp();
            if (mListener != null) mListener.onComplete(result);
        } catch (IOException ignored) {

        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (mListener != null) mListener.updateUI();
    }
}
