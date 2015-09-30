package com.pentapenguin.jvcbrowser.app;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.pentapenguin.jvcbrowser.R;

import java.io.*;
import java.lang.reflect.Field;

public class App extends Application {

    public static final String HOST_MOBILE = "http://m.jeuxvideo.com";
    public static final String HOST_WEB = "http://www.jeuxvideo.com";
    public static final String HOST_API = "http://api.jeuxvideo.com";
    public static final String APP_NAME = "JVC Browser";

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) App.sContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public static void hideKeyboard(IBinder binder) {
        InputMethodManager imm = (InputMethodManager) App.sContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static AlertDialog alert(Context context, int message) {
        if (message == R.string.no_response) return null;
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
        Toast.makeText(sContext, message, Toast.LENGTH_LONG).show();
    }

    public static void snack(View view, String message, String text, View.OnClickListener onClick) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(text, onClick)
                .show();
    }

    public static void snack(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    public static void snack(View view, int message) {
        Snackbar.make(view, sContext.getString(message), Snackbar.LENGTH_LONG).show();
    }

    private static String getFilePathFromGalery(Context context, Uri uri) {
        String filePath;
        Cursor cursor = context.getContentResolver()
                .query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
        cursor.moveToFirst();
        filePath = cursor.getString(0);
        cursor.close();
        return filePath;
    }

    private static String getFilePathFromImage(Context context, Uri uri) {
        String id = uri.getPath().split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};
        String sel = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);
        String filePath = null;
        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) filePath = cursor.getString(columnIndex);
        cursor.close();

        return filePath;
    }

    public static String getFilePath(Context context, Uri uri) {
        String fileName = getFilePathFromGalery(context, uri);
        if (fileName == null) fileName = getFilePathFromImage(context, uri);

        return fileName;
    }

    public static String getResourceAsset(String path) throws IOException {

        InputStream is = sContext.getResources().getAssets().open(path);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;

        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
        }
        is.close();

        return writer.toString();
    }

    public static int getRessourceId(Class cls, String variableName) throws NoSuchFieldException, IllegalAccessException {
        Field idField = cls.getDeclaredField(variableName);

        return idField.getInt(idField);
    }
}

