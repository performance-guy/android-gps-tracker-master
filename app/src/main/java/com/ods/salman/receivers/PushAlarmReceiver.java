package com.ods.salman.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ods.salman.services.PushLocationService;

public class PushAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_PUSH_LOCATION_ALARM =
            "com.ods.salman.ACTION_PUSH_LOCATION_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, PushLocationService.class);
        context.startService(startIntent);
    }
}
