package lb.themike10452.purityu2d;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.themike10452.purityu2d.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import lb.themike10452.purityu2d.services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/26/2014.
 */
public class mBroadcastReceiver extends BroadcastReceiver {

    private static Context context;
    private static SharedPreferences preferences;
    private static String DEVICE_PART;

    @Override
    public void onReceive(Context c, Intent intent) {
        context = c;
        preferences = context.getSharedPreferences(Keys.SharedPrefsKey, Context.MODE_PRIVATE);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true)) {
                context.startService(new Intent(context, BackgroundAutoCheckService.class));
            }
        } else if (BackgroundAutoCheckService.ACTION.equals(intent.getAction()) && !MainActivity.isVisible && !Tools.isDownloading) {
            new Thread(run).start();
        }
    }

    //this is the background check process
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            boolean DEVICE_SUPPORTED = true;
            boolean CONNECTED = false;
            try {
                CONNECTED = getDevicePart();
            } catch (DeviceNotSupportedException e) {
                DEVICE_SUPPORTED = false;
            }

            //if the device is not supported, kill the task
            if (!DEVICE_SUPPORTED) {
                stopSelf();
                return;
            }

            if (CONNECTED) { //if the phone was connected by the time, we need to check for an update

                //get installed and latest kernel info, and compare them
                Tools.getBuildVersion();
                String installed = Tools.INSTALLED_ROM_VERSION;
                Tools.sniffBuilds(DEVICE_PART);
                Build properKernel = BuildManager.getInstance().getProperBuild(context.getApplicationContext());
                String latest = properKernel != null ? properKernel.getVERSION() : null;

                //if the user hasn't opened the app and selected which ROM base he wants (AOSP/CM)
                //latest will be null
                //we should stop our work until the user sets the missing ROM flag
                if (latest == null) {
                    stopSelf();
                    return;
                }

                //display a notification to the user in case of an available update
                try {
                    int cv = Integer.parseInt(installed);
                    int lv = Integer.parseInt(latest);
                    if (cv >= lv)
                        return;
                } catch (Exception e){
                    if (installed.equalsIgnoreCase(latest))
                        return;
                }

                Intent intent1 = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notif = new Notification.Builder(context.getApplicationContext())
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.msg_updateFound)).build();
                notif.flags = Notification.FLAG_AUTO_CANCEL;
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(Keys.TAG_NOTIF, 3721, notif);
            }
        }
    };

    private void stopSelf() {
        context.stopService(new Intent(context, BackgroundAutoCheckService.class));
    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s = null;
        HttpURLConnection connection = null;
        DEVICE_PART = "";
        try {
            try {
                if (preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false)) {
                    final String proxyHost = preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY);
                    System.setProperty("http.proxySet", "true");
                    System.setProperty("http.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                    System.setProperty("http.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
                    System.setProperty("https.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                    System.setProperty("https.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
                } else {
                    System.setProperty("http.proxySet", "false");
                }
                connection = (HttpURLConnection) new URL(preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE)).openConnection();
                s = new Scanner(connection.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            String pattern = String.format("<%s>", android.os.Build.DEVICE);

            boolean supported = false;
            while (s.hasNextLine()) {
                if (s.nextLine().equalsIgnoreCase(pattern)) {
                    supported = true;
                    break;
                }
            }
            if (supported) {
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    if (line.equalsIgnoreCase(String.format("</%s>", android.os.Build.DEVICE)))
                        break;
                    DEVICE_PART += line + "\n";
                }
                return true;
            } else {
                throw new DeviceNotSupportedException();
            }
        } finally {
            if (s != null)
                s.close();
            if (connection != null)
                connection.disconnect();
        }

    }
}
