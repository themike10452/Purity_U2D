package com.themike10452.purityu2d;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by Mike on 4/24/2014.
 */
public class AutoCheckService extends Service {

    public static boolean loop;
    public final static String ACTION_RECEIVE_UPDATE = "U2D_receive@10452";
    private final String NOTIFICATION_TAG = "U2D";
    private String currentVersion, latestVersion, device;
    private int NOTIFICATION_ID = 10452;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "Service Started");
        loop = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (loop) {
                    check();
                    try {
                        Thread.sleep(3600000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loop = false;
    }

    private void check() {
        new AsyncTask<Void, Void, Void>() {
            private String cv;

            @Override
            protected Void doInBackground(Void... voids) {
                cv = lib.shellOut(String.format("cat %s | grep %s ", lib.buildProp, lib.versionPropTag), "=", 1);
                try {
                    currentVersion = cv.split("\\.")[2];
                } catch (IndexOutOfBoundsException e) {
                    currentVersion = "n/a";
                }
                try {
                    device = lib.shellOut(String.format("cat %s | grep %s", lib.buildProp, lib.devicePropTag), "=", 1)
                            .toLowerCase()
                            .trim();
                } catch (Exception e) {
                    stopSelf();
                }
                final File HOST = new File(getFilesDir() + File.separator + "host");
                final String host_file = ((new File(getFilesDir() + "/enable_developer")).exists())? lib.test_host : lib.host;
                new FileDownloader(getApplicationContext(), host_file, HOST, true, true) {
                    @Override
                    protected void onPostExecute(Boolean successful) {
                        super.onPostExecute(successful);
                        if (successful) {
                            try {
                                BufferedReader reader = new BufferedReader(new FileReader(HOST));
                                String line;
                                boolean found = false;
                                while ((line = reader.readLine()) != null) {
                                    if (line.toLowerCase().contains(device + "=")) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    latestVersion = line.substring(line.indexOf("=") + 1, line.indexOf(">>")).trim();
                                    int i = latestVersion.compareTo(currentVersion);
                                    Log.d("TAG", i + " : " + latestVersion + " : " + currentVersion);
                                    if (i > 0) {
                                        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                        Notification.Builder builder = new Notification.Builder(getApplicationContext());
                                        Intent notify = new Intent(ACTION_RECEIVE_UPDATE);
                                        notify.putExtra("line", line);
                                        PendingIntent intent = PendingIntent.getBroadcast(getApplicationContext(), 0, notify, 0);
                                        IntentReceiver receiver = new IntentReceiver();
                                        registerReceiver(receiver, new IntentFilter(ACTION_RECEIVE_UPDATE));
                                        builder.setContentTitle(getString(R.string.message_update))
                                                .setContentText(getString(R.string.message_clickReceive))
                                                .setSmallIcon(R.drawable.purity)
                                                .setContentIntent(intent);
                                        manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, builder.build());
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }.execute();

                return null;
            }
        }.execute();
    }
}
