package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import org.jsoup.Connection;

import java.io.IOException;

public class ProfileFragment extends Fragment {

    public static final String PROFILE_ARG = "profile_arg";
    public static final String TAG = "profile";
    public static final String URL = "http://m.jeuxvideo.com/profil/?.html";

    private WebView mContent;
    private String mPseudo;

    public static ProfileFragment newInstance(String pseudo) {
        ProfileFragment fragment = new ProfileFragment();

        Bundle args = new Bundle();
        args.putString(PROFILE_ARG, pseudo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPseudo = getArguments().getString(PROFILE_ARG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_profile, container, false);
        mContent = (WebView) layout.findViewById(R.id.profile_content);

        mContent.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        String url = URL.replace("?", mPseudo);

        dialog.show();
        Ajax.url(url).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                dialog.dismiss();
                if (response != null) {
                    try {
                        mContent.loadDataWithBaseURL("file:///android_asset/", Parser.profileContent(response.parse()),
                                "text/html", "utf-8", null);
                    } catch (IOException e) {
                        App.alert(getActivity(), e.getMessage());
                    } catch (NoContentFoundException e) {
                        App.alert(getActivity(), e.getMessage());
                    }
                } else {
                    dialog.dismiss();
                    App.alert(getActivity(), R.string.no_response);
                }
            }
        }).execute();
    }
}
