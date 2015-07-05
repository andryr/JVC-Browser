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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;

public class EditFragment extends Fragment{

    public static final String TAG = "edit";
    public static final String URL = "http://www.jeuxvideo.com/forums/ajax_edit_message.php";
    public static final String EDIT_TOPIC_ARG = "edit_topic";
    private static final String CLASS_FORM_EDIT = "form-post-topic";
    private static final String CLASS_ERROR = "alert-danger";
    private static final String CLASS_CAPTCHA = "bloc-captcha";
    private static final String CONTENT_SAVE = "content";

    private ImageView mCaptcha;
    private EditText mContent;
    private TextView mError;
    private EditText mCode;
    private Button mPost;
    private HashMap<String, String> mData;
    private Topic mTopic;
    private EditObserver mListener;

    public static EditFragment newInstance(Topic topic) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();

        args.putParcelable(EDIT_TOPIC_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (EditObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTopic = getArguments().getParcelable(EDIT_TOPIC_ARG);
        mData = new HashMap<String, String>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_edit, container, false);

        mCaptcha = (ImageView) layout.findViewById(R.id.edit_captcha);
        mContent = (EditText) layout.findViewById(R.id.edit_content);
        mError = (TextView) layout.findViewById(R.id.edit_error);
        mCode = (EditText) layout.findViewById(R.id.edit_code);
        mPost = (Button) layout.findViewById(R.id.edit_post);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Document doc = Jsoup.parse(mTopic.getContent());
        mData = Parser.hidden(doc, CLASS_FORM_EDIT);

        if (savedInstanceState != null) mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
        mData.put("ajax_hash", mTopic.getAuthor());
        mData.put("ajax_timestamp", mTopic.getLastPostDate());
        mData.put("action", "post");
        mData.put("id_message", Integer.toString(mTopic.getPostsNumber()));
        String captcha = Parser.captcha(doc, "col-md-4");
        if (captcha != null) onCaptcha(captcha);
        mContent.append(Parser.textArea(doc));

        mPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

                String content = checkField(mContent);
                String code = checkField(mCode);

                if (content.equals("")) {
                    App.alert(getActivity(), R.string.field_empty);
                    return;
                }
                dialog.setMessage("Post en cours...");
                dialog.show();
                initWidget();
                mData.put("message_topic", content);
                if (!code.equals("")) mData.put("fs_ccode", code);
                Ajax.url(URL).data(mData).ignoreContentType(true).cookie(Auth.COOKIE_NAME,
                        Auth.getInstance().getCookieValue()).post().callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        dialog.dismiss();
                        if (response != null) {
                            String error = json(response.body());

                            if (error != null) {
                                App.alert(getActivity(), error);
                            } else {
                                onPost(null);
                            }
                        } else {
                            App.alert(getActivity(), R.string.no_response);
                        }
                    }

                    private String json(String message) {
                        try {
                            JSONObject json = new JSONObject(message);
                            JSONArray errors = json.getJSONArray("erreur");
                            if (errors.length() != 0) {
                                return errors.get(0).toString();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }).execute();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_SAVE, mContent.getText().toString());
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
        mPost.setEnabled(false);
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

    public interface EditObserver {
        void onPost();
    }
}
