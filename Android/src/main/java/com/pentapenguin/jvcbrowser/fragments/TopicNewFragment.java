package com.pentapenguin.jvcbrowser.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.SmileysActivity;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.ItemPosted;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.squareup.okhttp.*;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class TopicNewFragment extends Fragment {

    public static final String URL = "http://m.jeuxvideo.com/forums/create_topic.php?id_forum=";
    public static final String TAG = "topic_new";
    public static final String TOPIC_NEW_ARG = "new_topic_arg";
    private static final String TITLE_SAVE = "title_save";
    private static final String CONTENT_SAVE = "content_save";
    private static final String CLASS_FORM = "form-post-msg";
    private static final String CLASS_ERROR = "alert-danger";
    private static final String CLASS_CAPTCHA = "bloc-captcha";
    public static final int REQUEST_CODE = 737;

    private EditText mTitle;
    private EditText mContent;
    private TextView mError;
    private ImageView mCaptcha;
    private EditText mCode;
    private Button mPost;
    private Forum mForum;
    private HashMap<String, String> mData;
    private ItemPosted mListener;

    public static TopicNewFragment newInstance(Forum forum) {
        TopicNewFragment fragment = new TopicNewFragment();
        Bundle args = new Bundle();

        args.putParcelable(TOPIC_NEW_ARG, forum);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (ItemPosted) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mForum = getArguments().getParcelable(TOPIC_NEW_ARG);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.topicNewFragment, container, false);

        mTitle = (EditText) layout.findViewById(R.id.new_topic_title_edit);
        mCaptcha = (ImageView) layout.findViewById(R.id.new_topic_captcha_image);
        mContent = (EditText) layout.findViewById(R.id.new_topic_content_edit);
        mError = (TextView) layout.findViewById(R.id.new_topic_error_text);
        mCode = (EditText) layout.findViewById(R.id.new_topic_code_edit);
        mPost = (Button) layout.findViewById(R.id.new_topic_post_button);
        mData = new HashMap<String, String>();

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
        if (savedInstanceState != null) {
            mTitle.setText(savedInstanceState.getString(TITLE_SAVE));
            mContent.setText(savedInstanceState.getString(CONTENT_SAVE));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE_SAVE, mTitle.getText().toString());
        outState.putString(CONTENT_SAVE, mContent.getText().toString());
    }

    // Copy/Paste from topicfragment

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && data != null) {
            Uri uri = data.getData();
            String filePath = App.getFilePath(getActivity(), uri);
            noelshackUpload(filePath);
        } else if (resultCode == SmileysActivity.RESULT_CODE && data != null) {
            mContent.append(" " + data.getStringExtra("smiley"));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_topic_new, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_topic_new_noelshack:
                noelshack();
                return true;
            case R.id.menu_topic_new_smileys:
                smileys();
                return true;
            case R.id.menu_topic_new_toolbar:
                toolbar();
                return true;
        }
        return false;
    }

    private void toolbar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.toolbar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String what;
                switch (which) {
                    case 0:
                        what = "'''  '''";
                        break;
                    case 1:
                        what = "''  ''";
                        break;
                    case 2:
                        what = "<u>  </u>";
                        break;
                    case 3:
                        what = "<s>  </s>";
                        break;
                    case 4:
                        what = "* ";
                        break;
                    case 5:
                        what = "# ";
                        break;
                    case 6:
                        what = "> ";
                        break;
                    case 7:
                        what = "<code>  </code>";
                        break;
                    case 8:
                        what = "<spoil>  </spoil>";
                        break;
                    default:
                        what = "";
                }
                mContent.append(what);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void smileys() {
        startActivityForResult(new Intent(getActivity(), SmileysActivity.class), 1000);
    }

    private void noelshack() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Selectionnez une image"), REQUEST_CODE);
    }

    private void noelshackUpload(String path) {
        final OkHttpClient client = new OkHttpClient();
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        dialog.show();
        File file = new File(path);
        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("fichier", file.getName(), RequestBody.create(MediaType.parse("image/*"), file))
                .build();

        Request request = new Request.Builder()
                .url("http://www.noelshack.com/api.php")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                App.alert(getActivity(), R.string.no_response);
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mContent.append("\n" + response.body().string());
                                if (dialog.isShowing()) dialog.dismiss();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    //

    private void init() {
        final String url = URL + Integer.toString(mForum.getId());
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

        dialog.show();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                dialog.dismiss();
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String captcha = Parser.captcha(doc, CLASS_CAPTCHA);
                        String error = Parser.error(doc, CLASS_ERROR);

                        if (captcha != null) onCaptcha(captcha);
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
                String title = checkField(mTitle);
                String content = checkField(mContent);
                String code = checkField(mCode);

                if (title.equals("") || content.equals("")) {
                    App.alert(getActivity(), R.string.field_empty);
                    return;
                }
                dialog.setMessage("Post en cours...");
                dialog.show();
                initWidget();
                mData.put("titre_topic", title);
                mData.put("message_topic", content);
                mData.put("form_alias_rang", "1");
                if (!code.equals("")) mData.put("fs_ccode", code);
                Ajax.url(url).data(mData).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).post()
                        .callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                dialog.dismiss();
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
        });
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
        if (mListener != null) mListener.onPost(topic);
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
