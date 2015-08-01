package com.pentapenguin.jvcbrowser.util.network;

import android.os.AsyncTask;

import java.io.*;
import java.net.*;

public class AjaxSocket extends AsyncTask<Void, Void, String> {

    public static final int PORT = 80;

    private String mUrl;
    private AjaxRawCallback mListener;

    public AjaxSocket(String mUrl, AjaxRawCallback mListener) {
        this.mUrl = mUrl;
        this.mListener = mListener;
    }

    private String getHttp() throws IOException {
        Socket s;
        PrintWriter s_out;
        BufferedReader s_in ;
        String result = "";

        try {
            s = new Socket(mUrl, PORT);
            s_out = new PrintWriter( s.getOutputStream(), true);
            s_in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (UnknownHostException e) {
            return null;
        }
        String message = "GET / HTTP/1.1\r\n\r\n";
        s_out.println( message );
        String response;

        while ((response = s_in.readLine()) != null) result += response;
        s.close();

        return result;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return getHttp();
        } catch (IOException ignored) {

        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (mListener != null) mListener.onComplete(s);
    }


}
