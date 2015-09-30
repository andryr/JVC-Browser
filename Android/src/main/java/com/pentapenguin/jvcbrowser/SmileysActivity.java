package com.pentapenguin.jvcbrowser;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.jsoup.Jsoup;

import java.io.IOException;

public class SmileysActivity extends AppCompatActivity {

    public static final int RESULT_CODE = 555;

    private final OkHttpClient mClient = new OkHttpClient();

    @SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface" })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WebView web = new WebView(this);

        web.setWebChromeClient(new WebChromeClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void onClicked(String smiley) {
                Intent intent = new Intent();
                intent.putExtra("smiley", smiley);
                setResult(RESULT_CODE, intent);
                finish();
            }

        }, "android");
        Request request = new Request.Builder()
                .url("http://m.jeuxvideo.com/forums/smiley.php")
                .build();
        final ProgressDialog dialog = App.progress(this, R.string.in_progress, true);
        dialog.show();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (dialog.isShowing()) dialog.dismiss();
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                final String smileys = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isShowing()) dialog.dismiss();
                        String p = Parser.smileys(Jsoup.parse(smileys));
                        web.loadDataWithBaseURL("file:///android_asset/", p, "text/html", "utf-8", null);
                    }
                });
            }
        });
        setContentView(web);
    }
}
