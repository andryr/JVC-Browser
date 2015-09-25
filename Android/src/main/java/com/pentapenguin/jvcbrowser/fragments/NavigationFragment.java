package com.pentapenguin.jvcbrowser.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.pentapenguin.jvcbrowser.R;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.app.Theme;
import com.pentapenguin.jvcbrowser.app.navigation.NavigationMenu;
import com.pentapenguin.jvcbrowser.entities.Navigation;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import com.pentapenguin.jvcbrowser.services.UpdateService;
import com.pentapenguin.jvcbrowser.util.ActivityLauncher;
import com.pentapenguin.jvcbrowser.util.FragmentLauncher;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;
import com.pentapenguin.jvcbrowser.util.widgets.CircularImageView;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerItemListener;
import com.pentapenguin.jvcbrowser.util.widgets.RecyclerViewAdapter;
import com.squareup.picasso.Picasso;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;

public class NavigationFragment extends Fragment {

    private static final int MP_POSITION = 11;
    private static final int NOTIFICATION_POSITION = 8;

    public enum NavigationType { Header, Category, Item }

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView mRecycler;
    private NavigationAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new NavigationAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(Theme.navigationFragment, container, false);

        mRecycler = (RecyclerView) layout.findViewById(R.id.navigation_list);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.addOnItemTouchListener(new RecyclerItemListener(getActivity(), mAdapter,
                new RecyclerItemListener.RecyclerItemGestureListener() {

                    @Override
                    public void onClick(Object item, int position) {
                        Navigation navigation = (Navigation) item;
                        if (navigation.getType() == NavigationType.Item) {
                            if (position == 1 && Auth.getInstance().isConnected()) {
                                disconnect();
                            } else if (navigation.getFragment() != null) {
                                getActivity().getSupportFragmentManager().popBackStack(null,
                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                ((FragmentLauncher) getActivity()).launch(navigation.getFragment(), false);
                            } else if (navigation.getIntent() != null) {
                                ((ActivityLauncher) getActivity()).launch(navigation.getIntent());
                            }
                        }
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    }
                }));

        return layout;
    }

    private void disconnect() {
        Auth.getInstance().disconnect();
        getActivity().getSupportFragmentManager().popBackStack(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ((FragmentLauncher) getActivity()).launch(ForumListFragment.newInstance(), false);
        mAdapter.load();
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getService(getActivity(), 0, new Intent(getActivity(), UpdateService.class),
                PendingIntent.FLAG_NO_CREATE));
        App.toast(R.string.disconnected);
        Log.d("Alarm", "unset");
    }

    public void updateMp(int mpCount) {
        if (Auth.getInstance().isConnected()) {
            mAdapter.mValues.get(MP_POSITION).setDetails(Integer.toString(mpCount));
            Storage.getInstance().put(UpdateService.MP_STORAGE, mpCount);
            mAdapter.notifyItemChanged(MP_POSITION);
        }
    }

    public void updateNotifications(int notificationCount) {
        if (Auth.getInstance().isConnected()) {
            mAdapter.mValues.get(NOTIFICATION_POSITION).setDetails(Integer.toString(notificationCount));
            Storage.getInstance().put(UpdateService.NOTIFICATION_STORAGE, notificationCount);
            mAdapter.notifyItemChanged(NOTIFICATION_POSITION);
        }
    }

    public DrawerLayout setUp(DrawerLayout layout, Toolbar toolbar) {
        mDrawerLayout = layout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), layout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActivity().supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        return mDrawerLayout;
    }

    public void reload() {
        mAdapter.load();
    }

    private class NavigationAdapter extends RecyclerViewAdapter {

        private ArrayList<Navigation> mValues;
        private LayoutInflater mInfalter;

        public NavigationAdapter() {
            mValues = new ArrayList<Navigation>();
            mInfalter = getActivity().getLayoutInflater();
            load();
        }

        private void load() {
            mValues = NavigationMenu.create(Auth.getInstance().isConnected() ?
                    NavigationMenu.Type.Connected : NavigationMenu.Type.NotConnected);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            NavigationType type = NavigationType.values()[viewType];
            View view;
            ViewHolder holder = null;

            switch (type) {
                case Header:
                    view = mInfalter.inflate(Theme.navigationHeader, parent, false);
                    holder = new HeaderHolder(view);
                    break;
                case Category:
                    view = mInfalter.inflate(Theme.navigationCategory, parent, false);
                    holder = new CategoryHolder(view);
                    break;
                case Item:
                    view = mInfalter.inflate(Theme.navigationItem, parent, false);
                    holder = new ItemHolder(view);
                    break;
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NavigationType type = mValues.get(position).getType();

            switch (type) {
                case Header:
                    HeaderHolder header = (HeaderHolder) holder;
                    header.bind();
                    break;
                case Category:
                    CategoryHolder category = (CategoryHolder) holder;
                    category.bind(mValues.get(position));
                    break;
                case Item:
                    ItemHolder item = (ItemHolder) holder;
                    item.bind(mValues.get(position));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mValues.get(position).getType().ordinal();
        }

        @Override
        public Object itemAt(int position) {
            if (position < mValues.size()) return mValues.get(position);

            return null;
        }
    }

    private class HeaderHolder extends RecyclerView.ViewHolder {

        private TextView mPseudo;
        private de.hdodenhof.circleimageview.CircleImageView mProfil;
        private ImageView mBackground;

        public HeaderHolder(View view) {
            super(view);

            mPseudo = (TextView) view.findViewById(R.id.navigation_header_pseudo);
            mProfil = (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.navigation_header_profile);
            mBackground = (ImageView) view.findViewById(R.id.navigation_header_background);
        }

        public void bind() {
            if (Auth.getInstance().isConnected()) {
                mPseudo.setText(Auth.getInstance().getPseudo());
                String url = "http://www.jeuxvideo.com/profil/" + Auth.getInstance().getPseudo().toLowerCase().trim();
                url = url.replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
                Ajax.url(url).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                        .callback(new AjaxCallback() {
                            @Override
                            public void onComplete(Connection.Response response) {
                                if (response != null) {
                                    try {
                                        Document doc = response.parse();
                                        int mps = Parser.mpUnread(doc);
                                        int notifs = Parser.notificationUnread(doc);
                                        updateMp(mps);
                                        updateNotifications(notifs);
                                        String background = Parser.profilBackground(doc);
                                        if (background != null) {
                                            try {
                                                int width = (int) getActivity().getResources().getDisplayMetrics().density * 320;
                                                int height = (int) getActivity().getResources().getDisplayMetrics().density * 180;

                                                Picasso.with(getActivity()).load(background).centerCrop().
                                                        resize(width, height).into(mBackground);
                                            } catch (Exception ignored) {

                                            }
                                        }
                                        Picasso.with(getActivity()).load(Parser.profilThumb(doc)).into(mProfil);
                                    } catch (NoContentFoundException ignored) {
                                    } catch (IOException ignored) {
                                    } catch (IllegalArgumentException ignored) {

                                    }
                                }
                            }
                        }).execute();
            } else {
                mBackground.setImageResource(R.drawable.drawer_image);
                mPseudo.setText("");
                mProfil.setImageResource(R.drawable.profil);
            }
        }
    }

    private class CategoryHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public CategoryHolder(View view) {
            super(view);

            mContent = (TextView) view.findViewById(R.id.navigation_header_category);
        }

        public void bind(Navigation navigation) {
            mContent.setText(navigation.getContent());
        }
    }

    private class ItemHolder extends RecyclerView.ViewHolder {

        private ImageView mThumb;
        private TextView mContent;
        private TextView mDetails;

        public ItemHolder(View view) {
            super(view);

            mThumb = (ImageView) view.findViewById(R.id.navigation_item_icon);
            mContent = (TextView) view.findViewById(R.id.navigation_item_content);
            mDetails = (TextView) view.findViewById(R.id.navigation_item_details);
        }

        public void bind(Navigation navigation) {
            mContent.setText(navigation.getContent());
            mDetails.setText(navigation.getDetails());
            mThumb.setImageResource(navigation.getThumb());
            if (!navigation.getDetails().equals("")) {
                mDetails.setVisibility(View.VISIBLE);
            }
        }
    }
}
