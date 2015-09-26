package com.pentapenguin.jvcbrowser.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.pentapenguin.jvcbrowser.MainActivity;
import com.pentapenguin.jvcbrowser.R;
import com.pentapenguin.jvcbrowser.app.Auth;
import com.pentapenguin.jvcbrowser.util.Parser;
import com.pentapenguin.jvcbrowser.util.network.Ajax;
import com.pentapenguin.jvcbrowser.util.network.AjaxCallback;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class UpdateService extends IntentService {

    public static final String MP_STORAGE = "mp_storage";
    public static final String MP_ACTION = "mp_action";
    public static final String MP_ARG = "mp_arg";
    public static final String NOTIFICATION_STORAGE = "notification_storage";
    public static final String NOTIFICATION_ACTION = "notification_action";
    public static final String NOTIFICATION_ARG = "notification_arg";

    public UpdateService() {
        super("mp service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Auth.getInstance().isConnected()) return;

        updateMp();
    }

    private void updateSubscribe(Document doc) {
        String url = "http://www.jeuxvideo.com/abonnements/ajax/count_notification_nonlu.php";
        HashMap<String, String> data = new HashMap<String, String>();
        String[] infos = Parser.subscribeData(doc).split("#");

        data.put("ajax_timestamp", infos[0]);
        data.put("ajax_hash", infos[1]);
        Ajax.url(url).data(data).ignoreContentType(true).cookie(Auth.COOKIE_NAME, Auth.getInstance().getCookieValue())
                .callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        if (response != null) {
                            int nb = json(response.body());
                            int last = Storage.getInstance().get(NOTIFICATION_STORAGE, 0);
                            if (nb > last) sendSubscribeNotification(nb - last, nb);
                            Storage.getInstance().put(NOTIFICATION_STORAGE, nb);
                            Intent intent = new Intent(NOTIFICATION_ACTION);
                            intent.putExtra(NOTIFICATION_ARG, nb);
                            sendBroadcast(intent);
                        }
                    }

                    private int json(String message) {
                        try {
                            JSONObject json = new JSONObject(message);
                            return json.getInt("nb_notif");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return 0;
                    }
                }).execute();

    }

    private void sendSubscribeNotification(int newNotif, int allNotif) {
        StringBuilder message = new StringBuilder();
        message.append("Vous avez " + newNotif + " nouvelle")
                .append(newNotif == 1 ? "" : "s")
                .append(" notification")
                .append(newNotif == 1 ? "" : "s")
                .append(" (" + allNotif + " non lue")
                .append(allNotif == 1 ? "" : "s")
                .append(")");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(NOTIFICATION_ACTION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setAutoCancel(true)
                .setContentText(message.toString())
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(2, notification);
    }

    private void updateMp() {
        String url = "http://www.jeuxvideo.com/messages-prives/boite-reception.php";

        Ajax.url(url).cookie(Auth.getInstance().getCookieName(), Auth.getInstance().getCookieValue())
                .callback(new AjaxCallback() {
                    @Override
                    public void onComplete(Connection.Response response) {
                        if (response != null) {
                            try {
                                Document doc = response.parse();
                                updateSubscribe(doc);
                                int mps = Parser.mpUnread(doc);
                                int lastMps = Storage.getInstance().get(MP_STORAGE, 0);
                                if (mps > lastMps) sendMpNotification(mps - lastMps, mps);
                                Storage.getInstance().put(MP_STORAGE, mps);
                                Intent intent = new Intent(MP_ACTION);
                                intent.putExtra(MP_ARG, mps);
                                sendBroadcast(intent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).execute();
    }

    private void sendMpNotification(int unreadMp, int allMp) {
        StringBuilder message = new StringBuilder();
        message.append("Vous avez " + unreadMp + " nouveau")
                .append(unreadMp == 1 ? "" : "x")
                .append(" mp")
                .append(unreadMp == 1 ? "" : "s")
                .append(" (" + allMp + " non lu")
                .append(allMp == 1 ? "" : "s")
                .append(")");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MP_ACTION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(message.toString())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
}
