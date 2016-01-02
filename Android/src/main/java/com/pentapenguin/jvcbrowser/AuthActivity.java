package com.pentapenguin.jvcbrowser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class AuthActivity extends Activity {

    private static final String URL_LOGIN = App.HOST_MOBILE + "/sso/login.php";

    public static final int RESULT_CODE = 42;


    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Theme.authActivity);


        mWebView = (WebView) findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(URL_LOGIN);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String cookies = CookieManager.getInstance().getCookie(App.HOST_MOBILE);
                String[] temp = cookies.split(";");
                for (String ar1 : temp ){
                    if(ar1.contains(Auth.COOKIE_NAME)){
                        int i = ar1.indexOf("=")+1;
                        if(i<ar1.length()) {
                            String cookie = ar1.substring(i);
                            onConnected("", cookie);
                        }
                        break;
                    }
                }
            }
        });


    }








    private void onConnected(String pseudo, String cookie) {
        Auth.getInstance().connect(pseudo, cookie);
        App.toast(R.string.connected);
        setResult(RESULT_CODE);
        finish();
    }


}
