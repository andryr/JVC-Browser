package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
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
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;


// Same class as PostNewFragment
public class MpNewFragment extends Fragment {

    public static final String TAG = "mp_new";
    public static final String URL = "http://www.jeuxvideo.com/messages-prives/message.php?id=";
    public static final String POST_NEW_MP_ARG = "post_new_post_arg";
    private static final String CLASS_FORM = "form-post-topic";
    private static final String CLASS_ERROR = "alert-raw";
    private static final String CLASS_CAPTCHA = "bloc-cap";
    private static final String CONTENT_SAVE = "content";

    private MpNewObserver mListener;
    private EditText mContent;
    private EditText mCode;
    private ImageView mCaptcha;
    private TextView mError;
    private Button mPost;
    private HashMap<String, String> mData;
    private ProgressDialog mDialog;
    private Topic mTopic;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (MpNewObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
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
        if (savedInstanceState != null) mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_SAVE, mContent.getText().toString());
    }

    private void initData() {
        String url = URL + Integer.toString(mTopic.getId());

        mDialog.setMessage("Initialisation...");
        mDialog.show();
        initWidget();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                        String error = Parser.error(doc, CLASS_ERROR);

                        if (captcha != null) onCaptcha(captcha);
                        if (error != null) onError(error);
                        mData = Parser.hidden(doc, CLASS_FORM);
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
        String url = URL + Integer.toString(mTopic.getId());
        String content = checkField(mContent);
        String code = checkField(mCode);

        mDialog.setMessage("Post en cours...");
        mData.put("message", content);
        if (!code.equals("")) mData.put("fs_ccode", code);
        Ajax.url(url).data(mData).ignoreContentType(true).cookie(Auth.COOKIE_NAME,
                Auth.getInstance().getCookieValue()).post().callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                mDialog.dismiss();
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                        String error = Parser.error(doc, CLASS_ERROR);

                        mData = Parser.hidden(doc, CLASS_FORM);
                        if (captcha != null) onCaptcha(captcha);
                        if (error != null) {
                            onError(error);

                            return;
                        }
                        onPost();
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

    private void onPost() {
        mContent.setText("");
        App.hideKeyboard(mContent.getWindowToken());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onPost();
            }
        }, 1000);
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

    public void setTopic(Topic topic) {
        mTopic = topic;
    }

    public interface MpNewObserver {
        void onPost();
    }

}
