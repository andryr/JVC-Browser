package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.pentapenguin.jvcbrowser.EditActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.entities.Post;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.ItemPosted;
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
    public static final String POST_ID_ARG = "post_id";
    private static final String CLASS_FORM_EDIT = "form-post-topic";
    private static final String CONTENT_SAVE = "content";

    private ImageView mCaptcha;
    private EditText mContent;
    private TextView mError;
    private EditText mCode;
    private Button mPost;
    private HashMap<String, String> mData;
    private Topic mTopic;
    private int mPostId;
    private ProgressDialog mDialog;

    public static EditFragment newInstance(Topic topic, int postId) {
        EditFragment fragment = new EditFragment();
        Bundle args = new Bundle();

        args.putParcelable(EDIT_TOPIC_ARG, topic);
        args.putInt(POST_ID_ARG, postId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTopic = getArguments().getParcelable(EDIT_TOPIC_ARG);
        mPostId = getArguments().getInt(POST_ID_ARG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.editFragment, container, false);

        mCaptcha = (ImageView) layout.findViewById(R.id.edit_captcha);
        mContent = (EditText) layout.findViewById(R.id.edit_content);
        mError = (TextView) layout.findViewById(R.id.edit_error);
        mCode = (EditText) layout.findViewById(R.id.edit_code);
        mPost = (Button) layout.findViewById(R.id.edit_post);
        mDialog = App.progress(getActivity(), R.string.in_progress, true);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
        init();
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
                                init();
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

    private void init() {
        String url = Helper.topicToMobileUrl(mTopic).replace("http://m", "http://www");

        mData = new HashMap<String, String>();
        setEnabledWidgets(false);
        if (!mDialog.isShowing()) mDialog.show();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                if (response != null) {
                    String html = response.body();

                    mData.put("ajax_timestamp", Parser.timestamp(html));
                    mData.put("ajax_hash", Parser.hash1(html));
                    mData.put("action", "get");
                    mData.put("id_message", Integer.toString(mPostId));
                    Ajax.url(URL).data(mData).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                            .ignoreContentType(true).callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            if (response != null) {
                                String edit = json(response.body());
                                if (edit != null) {
                                    Document doc = Jsoup.parse(edit);
                                    mData.put("action", "post");
                                    mData.putAll(Parser.hidden(doc, CLASS_FORM_EDIT));
                                    String captcha = Parser.captcha(doc, "col-md-4");
                                    if (captcha != null) {
                                        init();
                                        return;
                                    }
                                    if (mDialog.isShowing()) mDialog.dismiss();
                                    if (mContent.getText().toString().equals("")) mContent.append(Parser.textArea(doc));
                                    setEnabledWidgets(true);

                                    return;
                                }
                                App.alert(getActivity(), R.string.edit_unavailable);
                                if (mDialog.isShowing()) mDialog.dismiss();
                                return;
                            }
                            App.alert(getActivity(), R.string.no_response);
                            mDialog.dismiss();
                        }

                        private String json(String message) {
                            try {
                                JSONObject json = new JSONObject(message);
                                JSONArray errors = json.getJSONArray("erreur");
                                if (errors.length() != 0) {
                                    return null;
                                }
                                return json.getString("html");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                    }).execute();

                    return;
                }
                App.alert(getActivity(), R.string.no_response);
                mDialog.dismiss();
            }
        }).execute();
    }

    private void setEnabledWidgets(boolean isEnabled) {
        mPost.setEnabled(isEnabled);
        mContent.setEnabled(isEnabled);
    }

    private void onCaptcha(String captcha) {
        mCaptcha.setVisibility(View.VISIBLE);
        mCode.setVisibility(View.VISIBLE);
        Picasso.with(getActivity()).load(captcha).into(mCaptcha);
    }

    private void onPost(final Topic topic) {
        mPost.setEnabled(false);
        ((ItemPosted) getActivity()).onPost(topic);
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
}
