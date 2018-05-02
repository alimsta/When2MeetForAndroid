package com.spring2018.cis350.group7.when2meetmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Specifies the behavior of the notification alarm receiver
 * Created by anniesu on 3/21/18.
 */

public class MyStartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent dailyUpdater = new Intent(context, MyService.class);
        context.startService(dailyUpdater);
        Log.d("AlarmReceiver", "Called context.startService from AlarmReceiver.onReceive");
    }
}
