package com.pentapenguin.jvcbrowser.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.*;
import android.widget.EditText;
import com.pentapenguin.jvcbrowser.EditActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.SmileysActivity;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.ItemPosted;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.squareup.okhttp.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class TopicFragment extends Fragment implements TopicPageFragment.TopicObserver, ItemPosted {

    public static final String PAGER_ARG = "pager_arg";
    public static final String TAG = "pager";
    public static final String CURRENT_PAGE_SAVE = "current_page";
    public static final String MAX_SAVE = "max";
    public static final String TITLE_SAVE = "title";
    public static final String POST_URL_SAVE = "post_url";
    public static final String POST_CONTENT_SAVE = "post_content";
    public static final String LOADED_SAVE = "loaded";
    public static final int REQUEST_CODE = 747;

    public enum TopicType { Title, Core}

    private Topic mTopic;
    private ViewPager mPager;
    private PagerAdapter mAdapter;
    private PostNewFragment mPost;
    private int mMax;
    private int mCurrentPage;
    private String mPostUrl;
    private String mTitle;
    private boolean mLoaded;
    private final OkHttpClient mClient = new OkHttpClient();

    public static TopicFragment newInstance(Topic topic) {
        TopicFragment fragment = new TopicFragment();
        Bundle args = new Bundle();

        args.putParcelable(PAGER_ARG, topic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopic = getArguments().getParcelable(PAGER_ARG);
        mMax = 1;
        mLoaded = false;
        mCurrentPage = mTopic.getPage() - 1;
        mTitle = null;

        if (savedInstanceState != null) {
            mMax = savedInstanceState.getInt(MAX_SAVE);
            mCurrentPage = savedInstanceState.getInt(CURRENT_PAGE_SAVE);
            mTitle = savedInstanceState.getString(TITLE_SAVE);
            mPostUrl = savedInstanceState.getString(POST_URL_SAVE);
            mLoaded = savedInstanceState.getBoolean(LOADED_SAVE);
            mPost = PostNewFragment.createInstance(savedInstanceState.getString(POST_CONTENT_SAVE));
            mPost.setPostUrl(mPostUrl);
        } else {
            mPost = PostNewFragment.createInstance("");
        }
        getChildFragmentManager().beginTransaction().replace(R.id.pager_post_new_layout, mPost, PostNewFragment.TAG)
                .commit();
        mAdapter = new PagerAdapter(getChildFragmentManager());
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.topicFragment, container, false);
        mPager = (ViewPager) layout.findViewById(R.id.topic_pager);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPager.setAdapter(mAdapter);
        ((TitleObserver) getActivity()).updateTitle("");
        if (mCurrentPage != 0) mPager.setCurrentItem(mCurrentPage);
        if (!Auth.getInstance().isConnected()) {
            getChildFragmentManager().beginTransaction().hide(mPost).commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_PAGE_SAVE, mPager.getCurrentItem());
        outState.putInt(MAX_SAVE, mMax);
        outState.putString(TITLE_SAVE, mTitle);
        outState.putBoolean(LOADED_SAVE, mLoaded);
        outState.putString(POST_URL_SAVE, mPostUrl);
        outState.putString(POST_CONTENT_SAVE, mPost.getContent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == EditActivity.RESULT_CODE) {
            reload(TopicPageFragment.Type.Edit);
        } else if(requestCode == REQUEST_CODE && data != null) {
            Uri uri = data.getData();
            String filePath = App.getFilePath(getActivity(), uri);
            noelshackUpload(filePath);
        } else if (resultCode == SmileysActivity.RESULT_CODE && data != null) {
            mPost.append(" " + data.getStringExtra("smiley"));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_topic, menu);
        menu.findItem(R.id.menu_topic_favorite).setVisible(Auth.getInstance().isConnected());
        menu.findItem(R.id.menu_topic_noelshack).setVisible(Auth.getInstance().isConnected());
        menu.findItem(R.id.menu_topic_smileys).setVisible(Auth.getInstance().isConnected());
        menu.findItem(R.id.menu_topic_toolbar).setVisible(Auth.getInstance().isConnected());
        menu.findItem(R.id.menu_topic_subscribe).setVisible(Auth.getInstance().isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pager_last:
                gotoLastPage();
                return true;
            case R.id.menu_topic_goto_page:
                gotoPage();
                return true;
            case R.id.menu_topic_favorite:
                addFavorite();
                return true;
            case R.id.menu_topic_noelshack:
                noelshack();
                return true;
            case R.id.menu_topic_subscribe:
                subscribe();
                return true;
            case R.id.menu_topic_smileys:
                smileys();
                break;
            case R.id.menu_topic_toolbar:
                toolbar();
                break;
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
                mPost.append(what);
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

    private void subscribe() {
        String url = Helper.topicToUrl(mTopic);
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

        dialog.show();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                if (response != null) {
                    try {
                        Document doc = response.parse();
                        String url = "http://www.jeuxvideo.com/abonnements/ajax/ajax_abo_insert.php";
                        String[] infos = Parser.subscribeData(doc).split("#");
                        HashMap<String, String> data = new HashMap<String, String>();

                        data.put("ajax_timestamp", infos[0]);
                        data.put("ajax_hash", infos[1]);
                        data.put("ids_liste", Integer.toString(Parser.idTopic(response.body())));
                        data.put("type", "topic");
                        Ajax.url(url).data(data).post().cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                .ignoreContentType(true).callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                dialog.dismiss();
                                if (response != null) {
                                    String result = json(response.body());
                                    if (result == null) {
                                        App.toast(R.string.subscribe_added);
                                        return;
                                    }
                                    App.alert(getActivity(), result);
                                } else {
                                    App.alert(getActivity(), R.string.no_response);
                                }
                            }

                            private String json(String message) {
                                try {
                                    JSONObject json = new JSONObject(message);
                                    String error = json.getString("error");
                                    if (!error.equals("")) return error;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }).execute();
                    } catch (IOException e) {
                        App.alert(getActivity(), e.getMessage());
                    }
                } else {
                    dialog.dismiss();
                    App.alert(getActivity(), R.string.no_response);
                }
            }
        }).execute();
    }

    private void noelshack() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Selectionnez une image"), REQUEST_CODE);
    }

    private void noelshackUpload(String path) {
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
        mClient.newCall(request).enqueue(new Callback() {
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
                                mPost.append("\n" + response.body().string());
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

    private void addFavorite() {
        final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);
        String url = Helper.topicToUrl(mTopic);

        dialog.show();
        Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).callback(new AjaxCallback() {
            @Override
            public void onComplete(Connection.Response response) {
                if (response != null) {
                    String url = App.HOST_WEB + "/forums/ajax_forum_prefere.php";
                    String html = response.body();
                    final HashMap<String, String> data = new HashMap<String, String>();

                    data.put("ajax_timestamp", Parser.timestamp(html));
                    data.put("ajax_hash", Parser.hash3(html));
                    data.put("id_forum", Integer.toString(mTopic.getIdForum()));
                    data.put("id_topic", Integer.toString(Parser.idTopic(html)));
                    data.put("type", "topic");
                    data.put("action", "add");

                    Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue()).ignoreContentType(true)
                            .data(data).callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            dialog.dismiss();
                            if (response != null) {
                                String result = json(response.body());
                                if (result == null) {
                                    App.toast(R.string.favorite_added);
                                    return;
                                }
                                App.alert(getActivity(), result);
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
                } else {
                    App.alert(getActivity(), R.string.no_response);
                    if (dialog.isShowing()) dialog.dismiss();
                }
            }
        }).execute();
    }

    private void gotoPage() {
        final EditText pageNumber = new EditText(getActivity());
        pageNumber.setHint("Numero de la page");
        pageNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(getActivity())
                .setView(pageNumber)
                .setTitle(R.string.app_name)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (pageNumber.getText().toString().equals("")) {
                            App.toast(R.string.field_empty);
                            return;
                        }
                        int page = Integer.parseInt(pageNumber.getText().toString());
                        if (page > 5000) {
                            App.toast(R.string.go_to_page_over_9000);
                            return;
                        }
                        mPager.setCurrentItem(page - 1);
                        App.hideKeyboard(pageNumber.getWindowToken());
                    }
                })
                .setNegativeButton("Fermer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        App.hideKeyboard(pageNumber.getWindowToken());
                        dialogInterface.dismiss();
                    }
                })
                .create().show();
    }

    @Override
    public void updatePostUrl(String postUrl) {
        mPostUrl = postUrl;
        if (mPost != null) mPost.setPostUrl(mPostUrl);
    }

    @Override
    public void gotoLastPage() {
        mPager.setCurrentItem(mAdapter.getCount(), true);
    }

    @Override
    public void updatePages(int max) {
        mAdapter.update(max);
        if (!mLoaded && mCurrentPage != 0) {
            mLoaded = true;
            mPager.setCurrentItem(mCurrentPage);
        }

    }

    @Override
    public void quote(Topic topic) {
        if (mPost != null) mPost.append(topic.getContent());
    }

    @Override
    public void reload(TopicPageFragment.Type type) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment != null && fragment instanceof TopicPageFragment) {
                ((TopicPageFragment) fragment).reload(type);
            }
        }
    }

    @Override
    public void onPost(Item item) {
        gotoLastPage();
        reload(TopicPageFragment.Type.Post);
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void update(int max) {
            if (mMax != max) {
                mMax = max;
                notifyDataSetChanged();
            }
        }

        @Override
        public Fragment getItem(int i) {
            return TopicPageFragment.newInstance(mTopic.page(i + 1));
        }

        @Override
        public int getCount() {
            return mMax;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (position + 1) + "";
        }
    }

}