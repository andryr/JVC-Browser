package com.pentapenguin.jvcbrowser.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import com.pentapenguin.jvcbrowser.MainActivity;
import com.pentapenguin.jvcbrowser.app.Auth;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            if (Auth.getInstance().isConnected()) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent alarm = PendingIntent.getService(context, 0, new Intent(context, UpdateService.class), 0);
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                        MainActivity.ALARM_INTERVAL, alarm);
            }
        }
    }

}
