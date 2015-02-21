package lb.themike10452.PurityU2D;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import lb.themike10452.PurityU2D.Services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/26/2014.
 */
public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = context.getSharedPreferences(Keys.SharedPrefsKey, Context.MODE_PRIVATE);
            if (preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true))
                context.startService(new Intent(context, BackgroundAutoCheckService.class));
        }
    }
}
