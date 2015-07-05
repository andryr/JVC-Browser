package com.pentapenguin.jvcbrowser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
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
    private static final String CLASS_FORM = "form-connect";
    private static final String CLASS_ERROR = "alert-danger";
    private static final String CLASS_CAPTCHA = "bloc-captcha";

    private HashMap<String, String> mData;
    private HashMap<String, String> mCookies;
    private Button mConnect;
    private EditText mPseudo;
    private EditText mPasswd;
    private ImageView mCaptcha;
    private EditText mCode;
    private TextView mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mData = new HashMap<String, String>();
        mCookies = new HashMap<String, String>();
        mConnect = (Button) findViewById(R.id.button_connect);
        mPseudo = (EditText) findViewById(R.id.text_pseudo);
        mPasswd = (EditText) findViewById(R.id.text_passwd);
        mCaptcha = (ImageView) findViewById(R.id.image_connect_captcha);
        mCode = (EditText) findViewById(R.id.edit_connect_captcha);
        mError = (TextView) findViewById(R.id.text_connect_error);

        init();

    }

    private void init() {
        final ProgressDialog dialog = App.progress(this, R.string.in_progress, true);

        dialog.show();
        Ajax.url(URL_LOGIN).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                dialog.dismiss();
                if (response != null) {
                    try {
                        mData.putAll(Parser.hidden(response.parse(), CLASS_FORM));
                        mCookies.putAll(response.cookies());

                        return;
                    } catch (IOException e) {
                        App.alert(AuthActivity.this, e.getMessage());
                    }
                }
                App.alert(AuthActivity.this, R.string.no_response);
            }
        }).execute();

        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.setMessage("Connexion en cours...");
                dialog.show();
                String psd = checkField(mPseudo);
                String pass = checkField(mPasswd);
                String code = checkField(mCode);

                mData.put("login_pseudo", psd);
                mData.put("login_password", pass);
                if (!code.equals("")) mData.put("fs_ccode", code);
                initWidgets();
                Ajax.url(URL_LOGIN).post().data(mData).cookies(mCookies).callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        dialog.dismiss();
                        if (response != null) {
                            try {
                                Document doc = response.parse();
                                String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                                String error = Parser.error(doc, CLASS_ERROR);

                                mData.clear();
                                mData.putAll(Parser.hidden(doc, CLASS_FORM));
                                if (captcha != null) onCaptcha(captcha);
                                if (error != null) onError(error);
                                for (String name : response.cookies().keySet()) {
                                    if (name.equals(Auth.COOKIE_NAME)) onConnected(response.cookies().get(name));
                                }

                                return;
                            } catch (IOException e) {
                                App.alert(AuthActivity.this, e.getMessage());
                            }
                        }
                        App.alert(AuthActivity.this, R.string.no_response);
                    }
                }).execute();
            }
        });
    }

    private String checkField(EditText edit) {
        if (edit.getText() != null && !edit.getText().toString().equals(""))
            return edit.getText().toString();
        return "";
    }

    private void initWidgets() {
        mCaptcha.setVisibility(View.GONE);
        mCode.setVisibility(View.GONE);
        mCode.setText("");
        mError.setVisibility(View.GONE);
    }

    private void onCaptcha(String url) {
        mCaptcha.setVisibility(View.VISIBLE);
        mCode.setVisibility(View.VISIBLE);
        Picasso.with(this).load(url).into(mCaptcha);
    }

    private void onConnected(String cookie) {
        Auth.getInstance().connect(checkField(mPseudo), cookie);
        App.toast(R.string.connected);
        setResult(777);
        finish();
    }

    private void onError(String error) {
        mError.setVisibility(View.VISIBLE);
        mError.setText(error);
    }
}
