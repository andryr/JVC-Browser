package com.pentapenguin.jvcbrowser.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.*;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.InboxNewActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Bans;
import com.pentapenguin.jvcbrowser.entities.Mp;
import com.pentapenguin.jvcbrowser.entities.Topic;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Helper;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.TitleObserver;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.widgets.*;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class InboxFragment extends Fragment{

    public static final String TAG = "inbox";
    public static final String CURRENT_PAGE_SAVE = "current_page";
    public static final String MP_NUMBER_SAVE = "mp_number";
    public static final String data_SAVE = "data";

    private RecyclerView2 mRecycler;
    private InboxAdapter mAdapter;
    private int mCurrentPage;
    private int mMpNumber;
    private HashMap<String, String> mData;

    public static InboxFragment newInstance() {
        return new InboxFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = new HashMap<String, String>();
        mCurrentPage = 1;
        mMpNumber = 0;

        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(CURRENT_PAGE_SAVE);
            mMpNumber = savedInstanceState.getInt(MP_NUMBER_SAVE);
            ArrayList<Mp> data = savedInstanceState.getParcelableArrayList(data_SAVE);
            mAdapter = new InboxAdapter(data);
        } else {
            mAdapter = new InboxAdapter();
        }
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_inbox, container, false);

        mRecycler = (RecyclerView2) layout.findViewById(R.id.inbox_mp_list);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setEmptyView(layout.findViewById(R.id.inbox_empty_text));
        mRecycler.setLoadingView(layout.findViewById(R.id.inbox_loading_bar));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {
                    @Override
                    public void onClick(Object item, int position) {

                        ((FragmentLauncher) getActivity()).launch(MpFragment.newInstance((Mp) item), true);
                    }

                    @Override
                    public void onLongClick(final Object item, int position) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.app_name)
                                .setIcon(R.mipmap.logo)
                                .setMessage("Supprimer ce MP ?")
                                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mAdapter.delete((Mp) item);
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }).create().show();
                    }
                }));
        ((TitleObserver) getActivity()).updateTitle(getActivity().getResources().getString(R.string.subtitle_inbox));

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_PAGE_SAVE, mCurrentPage);
        outState.putInt(MP_NUMBER_SAVE, mMpNumber);
        outState.putParcelableArrayList(data_SAVE, mAdapter.mValues);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_inbox, menu);
        menu.findItem(R.id.menu_inbox_previous).setVisible(mCurrentPage != 1);
        menu.findItem(R.id.menu_inbox_next).setVisible(mCurrentPage * 25 < mMpNumber);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_inbox_new_mp:
                newMP();
                return true;
            case R.id.menu_inbox_next:
                nextPage();
                return true;
            case R.id.menu_inbox_previous:
                prevPage();
                return true;
            case R.id.menu_inbox_refresh:
                mAdapter.load();
                return true;
        }
        return false;
    }

    private void prevPage() {
        mCurrentPage--;
        mAdapter.load();
    }

    private void nextPage() {
        mCurrentPage++;
        mAdapter.load();
    }

    private void newMP() {
        Intent intent = new Intent(getActivity(), InboxNewActivity.class);
        startActivityForResult(intent, 666);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == InboxNewActivity.RESULT_CODE) {
            Mp mp = data.getParcelableExtra(MpFragment.MP_ARG);
            ((FragmentLauncher) getActivity()).launch(MpFragment.newInstance(mp), true);
        }
    }

    private class InboxAdapter extends RecyclerViewAdapter<InboxHolder> {

        private ArrayList<Mp> mValues;

        public InboxAdapter() {
            mValues = new ArrayList<Mp>();
            load();
        }

        public InboxAdapter(ArrayList<Mp> values) {
            mValues = new ArrayList<Mp>(values);
        }

        public void load() {
            mValues.clear();
            notifyDataSetChanged();
            Ajax.url(Helper.inboxToUrl(mCurrentPage)).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                    .callback(new AjaxCallback() {
                        @Override
                        public void onComplete(Connection.Response response) {
                            if (response != null) {
                                try {
                                    Document doc = response.parse();
                                    mValues = Parser.inbox(doc);
                                    Iterator<Mp> it = mValues.iterator();

                                    while (it.hasNext()) {
                                        Topic topic = it.next();
                                        if (Bans.isBanned(topic.getAuthor())) it.remove();
                                    }
                                    if (mValues.size() == 0) throw new NoContentFoundException();
                                    mMpNumber = Parser.mpNumber(doc);
                                    if (getActivity() != null) getActivity().supportInvalidateOptionsMenu();
                                    mData = Parser.hidden(doc, "col-md-9");
                                    notifyDataSetChanged();

                                    return;
                                } catch (NoContentFoundException e) {
                                    App.alert(getActivity(), e.getMessage());
                                } catch (IOException e) {
                                    App.alert(getActivity(), e.getMessage());
                                }

                            } else {
                                App.alert(getActivity(), R.string.no_response);
                            }
                            mRecycler.showNoResults();
                        }
                    }).execute();
        }

        @Override
        public InboxHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_mp, parent, false);
            return new InboxHolder(view);
        }

        @Override
        public void onBindViewHolder(InboxHolder holder, int position) {
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

        private void delete(Mp mp) {
            final ProgressDialog dialog = App.progress(getActivity(), R.string.in_progress, true);

            dialog.show();
            mData.put("conv_select[]", Integer.toString(mp.getId()));
            mData.put("conv_move", "1337");
            Ajax.url(Helper.inboxToUrl(mCurrentPage)).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                    .post().data(mData).callback(new AjaxCallback() {
                @Override
                public void onComplete(Connection.Response response) {
                    dialog.dismiss();
                    if (response != null) {
                        mAdapter.load();
                        return;
                    }
                    App.alert(getActivity(), R.string.no_response);
                }
            }).execute();
        }
    }

    private class InboxHolder extends RecyclerView.ViewHolder {

        private TextView mContent;
        private TextView mDate;
        private TextView mAuthor;
        private TextView mLu;

        public InboxHolder(View view) {
            super(view);

            mContent = (TextView) view.findViewById(R.id.mp_item_content);
            mDate = (TextView) view.findViewById(R.id.mp_item_date);
            mAuthor = (TextView) view.findViewById(R.id.mp_item_author);
            mLu = (TextView) view.findViewById(R.id.mp_item_lu);
        }

        public void bind(final Mp mp) {
            mLu.setText(mp.getCode() == 1 ? "(Lu)" : Html.fromHtml("<b><font color=\"#000\">(Non lu)</font></b>"));
            mContent.setText(mp.getContent());
            mDate.setText(mp.getIdForum() == 0 ? Html.fromHtml("<font color=\"#ff0000\">[Locked] </font>") : "");
            mDate.append(mp.getLastPostDate());
            mAuthor.setText(mp.getAuthor());
        }
    }
}
