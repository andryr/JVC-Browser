package com.pentapenguin.jvcbrowser.app;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.pentapenguin.jvcbrowser.R;

public class App extends Application {

    public static final String HOST_MOBILE = "http://m.jeuxvideo.com";
    public static final String HOST_WEB = "http://www.jeuxvideo.com";
    public static final String HOST_API = "http://api.jeuxvideo.com";
    public static final String APP_NAME = "JVC Browser";

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) App.context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public static void hideKeyboard(IBinder binder) {
        InputMethodManager imm = (InputMethodManager) App.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static AlertDialog alert(Context context, int message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(APP_NAME)
                .setIcon(R.mipmap.logo)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();
        dialog.show();

        return dialog;
    }

    public static AlertDialog alert(Context context, String message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(APP_NAME)
                .setIcon(R.mipmap.logo)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();
        dialog.show();

        return dialog;
    }

    public static AlertDialog alertOkCancel(Context context, String message, DialogInterface.OnClickListener listener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(APP_NAME)
                .setIcon(R.mipmap.logo)
                .setMessage(message)
                .setPositiveButton("Ok", listener)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        dialog.show();

        return dialog;
    }

    public static ProgressDialog progress(Context context, int message, boolean indeterminate) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(getContext().getResources().getString(message));
        dialog.setIndeterminate(indeterminate);
        dialog.setTitle(APP_NAME);
        dialog.setIcon(R.mipmap.logo);
        return dialog;
    }

    public static void toast(int message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
