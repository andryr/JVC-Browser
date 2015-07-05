package com.pentapenguin.jvcbrowser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.fragments.ForumFragment;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.DividerItemDecoration;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerView2;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ForumSearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final RecyclerView2 recycler = (RecyclerView2) findViewById(R.id.forum_search_list);
        final EditText forumName = (EditText) findViewById(R.id.forum_search_name);
        Button search = (Button) findViewById(R.id.forum_search_button);
        final ForumSearchAdapter adapter = new ForumSearchAdapter();

        setSupportActionBar(toolbar);
        toolbar.setSubtitle("Recherche forum");
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recycler.addOnItemTouchListener(new RecyclerItemListener(adapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {
                        Intent intent = new Intent(ForumSearchActivity.this, ForumActivity.class);
                        intent.putExtra(ForumFragment.FORUM_ARG, (Forum) item);
                        startActivity(intent);
                    }
                }));
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = checkField(forumName);

                if (!name.equals("")) {
                    App.hideKeyboard(forumName.getWindowToken());
                    adapter.load(name);
                    return;
                }
                App.toast(R.string.field_empty);
            }

            private String checkField(EditText edit) {
                if (edit.getText() != null && !edit.getText().toString().equals(""))
                    return edit.getText().toString();
                return "";
            }
        });
    }

    private class ForumSearchAdapter extends RecyclerViewAdapter<ForumSearchHolder> {

        private ArrayList<Forum> mValues;
        private LayoutInflater mInflater;

        public ForumSearchAdapter() {
            mValues = new ArrayList<Forum>();
            mInflater = getLayoutInflater();
        }

        public void load(String forumName) {
            String url = "http://api.jeuxvideo.com/forums/search_forum.php?input_search_forum=" + forumName;
            final ProgressDialog dialog = App.progress(ForumSearchActivity.this, R.string.in_progress, true);

            dialog.show();
            mValues.clear();
            notifyDataSetChanged();
            Ajax.url(url).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    dialog.dismiss();
                    if (response != null) {
                        try {
                            mValues = Parser.searchForum(response.parse());
                            notifyDataSetChanged();
                        } catch (IOException e) {
                            App.alert(ForumSearchActivity.this, e.getMessage());
                        }

                    } else {
                        App.alert(ForumSearchActivity.this, R.string.no_response);
                    }
                }
            }).execute();

        }

        @Override
        public ForumSearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_forum_search, parent, false);
            return new ForumSearchHolder(view);
        }

        @Override
        public void onBindViewHolder(ForumSearchHolder holder, int position) {
            holder.bind(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }
    }

    private class ForumSearchHolder extends RecyclerView.ViewHolder {

        private TextView mContent;
        private ImageView mFavoris;

        public ForumSearchHolder(View itemView) {
            super(itemView);

            mContent = (TextView) itemView.findViewById(R.id.forum_search_item_content);
            mFavoris = (ImageView) itemView.findViewById(R.id.forum_search_item_favoris);
        }

        public void bind(final Forum forum) {
            mContent.setText(forum.getContent());
            mFavoris.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    add2Favoris();
                }

                private void add2Favoris() {
                    final ProgressDialog dialog = App.progress(ForumSearchActivity.this, R.string.in_progress, true);

                    dialog.show();
                    String url = Helper.forumToUrl(forum);
                    Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                            .callback(new AjaxCallback() {
                                @Override
                                public void onComplete(Connection.Response response) {
                                    if (response != null) {
                                        String html = response.body();
                                        String url = "http://www.jeuxvideo.com/forums/ajax_forum_prefere.php";
                                        String hash = Parser.hash3(html);
                                        String timestamp = Parser.timestamp(html);
                                        HashMap<String, String> values = new HashMap<String, String>();

                                        values.put("id_forum", Integer.toString(forum.getId()));
                                        values.put("id_topic", "0");
                                        values.put("action", "add");
                                        values.put("type", "forum");
                                        values.put("ajax_timestamp", timestamp);
                                        values.put("ajax_hash", hash);

                                        Ajax.url(url).data(values).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                                                .ignoreContentType(true).callback(new AjaxCallback() {
                                            @Override
                                            public void onComplete(Connection.Response response) {
                                                dialog.dismiss();
                                                if (response != null) {
                                                    String a = response.body();
                                                    String js = json(a);
                                                    if (js != null) {
                                                        App.alert(ForumSearchActivity.this, js);
                                                    } else {
                                                        App.toast(R.string.favorite_added);
                                                    }
                                                } else {
                                                    App.alert(ForumSearchActivity.this, R.string.no_response);
                                                }
                                            }

                                            private String json(String input) {
                                                try {
                                                    JSONObject json = new JSONObject(input);
                                                    JSONArray errors = json.getJSONArray("erreur");
                                                    if (errors.length() != 0) return errors.getString(0);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                return null;
                                            }
                                        }).execute();

                                    } else {
                                        dialog.dismiss();
                                        App.alert(ForumSearchActivity.this, R.string.no_response);
                                    }
                                }
                            }).execute();
                }
            });
        }
    }
}
