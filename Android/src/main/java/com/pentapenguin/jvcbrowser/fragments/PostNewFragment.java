package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.ItemPosted;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class PostNewFragment extends Fragment {

    public static final String TAG = "post_new";
    private static final String CLASS_FORM_NORMAL = "form-post-msg";
    private static final String CLASS_ERROR = "alert-danger";
    private static final String CLASS_CAPTCHA = "bloc-captcha";
    private static final String CONTENT_SAVE = "content";
    private static final String POST_URL_SAVE = "post_url";

    private EditText mContent;
    private EditText mCode;
    private ImageView mCaptcha;
    private TextView mError;
    private Button mPost;
    private HashMap<String, String> mData;
    private ProgressDialog mDialog;
    private String mPostUrl;

    public static PostNewFragment createInstance() {
        return new PostNewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_post_new, container, false);

        mContent = (EditText) layout.findViewById(R.id.new_post_content);
        mCode = (EditText) layout.findViewById(R.id.new_post_code);
        mCaptcha = (ImageView) layout.findViewById(R.id.new_post_captcha);
        mError = (TextView) layout.findViewById(R.id.new_post_error);
        mPost = (Button) layout.findViewById(R.id.new_post);
        mData = new HashMap<String, String>();
        mDialog = App.progress(getActivity(), R.string.in_progress, true);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDialog.setCancelable(false);
        mPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = checkField(mContent);

                if (content.equals("")) {
                    App.alert(getActivity(), R.string.field_empty);
                    return;
                }
                initData();
            }
        });
        if (savedInstanceState != null) {
            mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
            mPostUrl = savedInstanceState.getString(POST_URL_SAVE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_SAVE, mContent.getText().toString());
        outState.putString(POST_URL_SAVE, mPostUrl);
    }

    private void initData() {
        mDialog.setMessage("Initialisation...");
        mDialog.show();
        initWidget();
        Ajax.url(mPostUrl).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {

            @Override
            public void onComplete(Connection.Response response) {
                mContent.setEnabled(true);
                mContent.setHint("Message...");
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                        String error = Parser.error(doc, CLASS_ERROR);

                        if (captcha != null) onCaptcha(captcha);
                        if (error != null) onError(error);
                        mData = Parser.hidden(doc, CLASS_FORM_NORMAL);
                        sendPost();

                        return;
                    } catch (IOException e) {
                        App.alert(getActivity(), e.getMessage());
                    }
                } else {
                    App.alert(getActivity(), R.string.no_response);
                }
                mDialog.dismiss();
            }
        }).execute();
    }

    private void sendPost() {
        String content = checkField(mContent);
        String code = checkField(mCode);

        mDialog.setMessage("Post en cours...");
        mData.put("message_topic", content);
        if (!code.equals("")) mData.put("fs_ccode", code);
        Ajax.url(mPostUrl).data(mData).ignoreContentType(true).cookie(Auth.COOKIE_NAME,
                Auth.getInstance().getCookieValue()).post().callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                mDialog.dismiss();
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                        String error = Parser.error(doc, CLASS_ERROR);

                        mData = Parser.hidden(doc, CLASS_FORM_NORMAL);
                        if (captcha != null) onCaptcha(captcha);
                        if (error != null) {
                            onError(error);

                            return;
                        }
                        try {
                            onPost(Parser.newTopic(doc));
                        } catch (NoContentFoundException e) {
                            App.alert(getActivity(), e.getMessage());
                        }
                    } catch (IOException e) {
                        App.alert(getActivity(), e.getMessage());
                    }
                } else {
                    App.alert(getActivity(), R.string.no_response);
                }
            }
        }).execute();
    }

    private void onError(String error) {
        mError.setVisibility(View.VISIBLE);
        mError.setText(error);
    }

    private void onCaptcha(String captcha) {
        mCaptcha.setVisibility(View.VISIBLE);
        mCode.setVisibility(View.VISIBLE);
        Picasso.with(getActivity()).load(captcha).into(mCaptcha);
    }

    private void onPost(final Topic topic) {
        mContent.setText("");
        App.hideKeyboard(mContent.getWindowToken());
        ((ItemPosted) getParentFragment()).onPost(topic);
    }

    private String checkField(EditText edit) {
        if (edit.getText() != null && !edit.getText().toString().equals(""))
            return edit.getText().toString();
        return "";
    }

    private void initWidget() {
        mCaptcha.setVisibility(View.GONE);
        mCode.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
    }

    public void setPostUrl(String postUrl) {
        mPostUrl = postUrl;
    }

    public void append(String content) {
        mContent.append(content);
    }
}
