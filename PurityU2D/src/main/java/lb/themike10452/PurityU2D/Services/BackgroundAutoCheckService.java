package lb.themike10452.PurityU2D.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import lb.themike10452.PurityU2D.Keys;
import lb.themike10452.PurityU2D.Tools;
import lb.themike10452.PurityU2D.mBroadcastReceiver;

/**
 * Created by Mike on 9/26/2014.
 */
public class BackgroundAutoCheckService extends Service {

    public final static String ACTION = "PURITY-U2D@THEMIKE10452";
    private PendingIntent pi;

    public static boolean running = false;
    private static AlarmManager manager;

    public BackgroundAutoCheckService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //actual work starts here
        running = true;

        SharedPreferences preferences = getSharedPreferences(Keys.SharedPrefsKey, MODE_PRIVATE);

        //get the autocheck interval setting value
        String pref = preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");
        //handle any corruptions that might have happened to the value by returning to the default value (12h00m)
        if (!Tools.isAllDigits(pref.replace(":", ""))) {
            preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0").apply();
            pref = "12:0";
        }
        //extract the 'hours' part
        String hr = pref.split(":")[0];
        //extract the 'minutes' part
        String mn = pref.split(":")[1];

        //parse them into integers and transform the total amount of time into Milliseconds
        long T = ((Integer.parseInt(hr) * 3600) + (Integer.parseInt(mn) * 60)) * 1000;

        //run the check task at a fixed rate
        Intent intent = new Intent(this, mBroadcastReceiver.class);
        intent.setAction(ACTION);
        pi = PendingIntent.getBroadcast(this, 0, intent, 0);
        manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, T, pi);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (pi != null) try {
            manager.cancel(pi);
        } catch (Throwable ignored) {
        }
    }
}
