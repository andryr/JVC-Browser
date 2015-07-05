package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class InboxNewFragment extends Fragment {

    public static final String TAG = "inbox_new";
    public static final String URL = "http://www.jeuxvideo.com/messages-prives/nouveau.php";
    private static final String CLASS_FORM = "form-post-topic";
    private static final String CLASS_ERROR = "alert-row";
    private static final String CONTENT_SAVE = "content";
    private static final String TITLE_SAVE = "title";
    private static final String DESTINATION_SAVE = "destination";

    private InboxNewObserver mListener;
    private EditText mTitle;
    private EditText mContent;
    private TextView mError;
    private Button mPost;
    private HashMap<String, String> mData;
    private ProgressDialog mDialog;
    private AppCompatMultiAutoCompleteTextView mDestination;
    private ArrayAdapter<String> mAdapter;

    public static InboxNewFragment newInstance() {
        return new InboxNewFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (InboxNewObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_inbox_new, container, false);

        mTitle = (EditText) layout.findViewById(R.id.new_inbox_title_edit);
        mContent = (EditText) layout.findViewById(R.id.new_inbox_content_edit);
        mError = (TextView) layout.findViewById(R.id.new_inbox_error_text);
        mPost = (Button) layout.findViewById(R.id.new_inbox_post_button);
        mDestination = (AppCompatMultiAutoCompleteTextView) layout.findViewById(R.id.new_inbox_destination_auto);
        mData = new HashMap<String, String>();
        mDialog = App.progress(getActivity(), R.string.in_progress, true);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
            mTitle.setText(savedInstanceState.getString(TITLE_SAVE));
            mDestination.setText(savedInstanceState.getString(DESTINATION_SAVE));
        }

        mDestination.setAdapter(mAdapter);
        mDestination.setThreshold(3);
        mDestination.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String input = charSequence.toString();
                String[] alias = input.split(",");
                final String url = "http://www.jeuxvideo.com/sso/ajax_suggest_pseudo.php?pseudo=" +
                        alias[alias.length - 1].trim();

                Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                        .ignoreContentType(true).callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        if (response != null) {
                            ArrayList<String> pseudos = json(response.body());

                            mAdapter.clear();
                            if (pseudos != null) {
                                for (String pseudo : pseudos) {
                                    mAdapter.add(pseudo);
                                }
                            }
                        }
                    }
                }).execute();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

            public ArrayList<String> json(String input) {
                try {
                    ArrayList<String> pseudos = new ArrayList<String>();
                    JSONObject json = new JSONObject(input);
                    JSONArray alias = json.getJSONArray("alias");
                    for (int i = 0; i < alias.length(); i++) {
                        pseudos.add(alias.getJSONObject(i).getString("pseudo"));
                    }

                    return pseudos;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        init();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_SAVE, mContent.getText().toString());
        outState.putString(TITLE_SAVE, mTitle.getText().toString());
        outState.putString(DESTINATION_SAVE, mDestination.getText().toString());
    }

    private void init() {
        mDialog.show();
        Ajax.url(URL).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                mDialog.dismiss();
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String error = Parser.error(doc, CLASS_ERROR);

                        if (error != null) onError(error);
                        mData.putAll(Parser.hidden(doc, CLASS_FORM));

                        return;
                    } catch (IOException e) {
                        App.alert(getActivity(), e.getMessage());
                    }
                }
                App.alert(getActivity(), R.string.no_response);
            }
        }).execute();
        mPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialog.setMessage("Post en cours...");
                mDialog.show();
                String title = checkField(mTitle);
                String content = checkField(mContent);
                String[] dests = checkField(mDestination).trim().split(",");
                ArrayList<Connection.KeyVal> data = new ArrayList<Connection.KeyVal>();

                initWidget();
                mData.put("conv_titre", title);
                mData.put("message", content);
                for(Map.Entry<String, String> entrey: mData.entrySet()) {
                    data.add(HttpConnection.KeyVal.create(entrey.getKey(), entrey.getValue()));
                }
                for (String dest : dests) {
                    data.add(HttpConnection.KeyVal.create("conv_dest[]", dest.trim()));
                }

                Ajax.url(URL).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                        .data(data).post().callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        mDialog.dismiss();
                        if (response != null) {
                            try {
                                Document doc = response.parse();
                                String error = Parser.error(doc, CLASS_ERROR);

                                mData = Parser.hidden(doc, CLASS_FORM);
                                if (error != null) {
                                    onError(error);
                                    return;
                                }
                                try {
                                    onPost(Parser.newMP(doc));
                                } catch (NoContentFoundException e) {
                                    App.alert(getActivity(), e.getMessage());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).execute();
            }
        });
    }


    private void onError(String error) {
        mError.setVisibility(View.VISIBLE);
        mError.setText(error);
    }

    private void onPost(final Topic topic) {
        mPost.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onPost(topic);
            }
        }, 1000);
    }

    private String checkField(EditText edit) {
        if (edit.getText() != null && !edit.getText().toString().equals(""))
            return edit.getText().toString();
        return "";
    }

    private void initWidget() {
        mError.setVisibility(View.GONE);
    }

    public interface InboxNewObserver {

        void onPost(Topic topic);

    }
}
