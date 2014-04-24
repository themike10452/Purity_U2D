package com.themike10452.purityu2d;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Mike on 4/24/2014.
 */
public class IntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, AutoCheckService.class));
        } else if (action.equals(AutoCheckService.ACTION_RECEIVE_UPDATE)) {
            String line = intent.getExtras().getString("line");
            Intent i = new Intent(context, DownloadActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("0x0", line.split("=")[1]);
            context.startActivity(i);
        }
    }
}
