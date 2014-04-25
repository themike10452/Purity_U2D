package com.themike10452.purityu2d;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DownloadService extends Service {


    public static boolean download_in_progress;
    public DownloadManager downloadManager;
    public long queueID;
    private String zip_name, http_url, OPTIONS, NOTIFICATION_TAG = "U2D";
    private int NOTIFICATION_ID = 10452;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        OPTIONS = "";

        if (intent != null && intent.getExtras().containsKey("0x0")) {
            zip_name = intent.getExtras().getString("0x0").trim();
        }
        if (intent != null && intent.getExtras().containsKey("0x1")) {
            http_url = intent.getExtras().getString("0x1");
        }
        if (intent != null && intent.getExtras().containsKey("0x2")) {
            //maintain kernel
            if (intent.getExtras().getBoolean("0x2"))
                OPTIONS += lib.ACTION_MAINTAIN_KERNEL;
        }
        if (intent != null && intent.getExtras().containsKey("0x3")) {
            //wipe cache and dalvik
            if (intent.getExtras().getBoolean("0x3"))
                OPTIONS += lib.ACTION_CLEAR_CACHE;
        }
        Log.d("TAG", OPTIONS);

        if (zip_name != null && http_url != null) {
            Receiver receiver = new Receiver();
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            registerReceiver(receiver, new IntentFilter(lib.FLAG_ACTION_REBOOT));
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(http_url.trim()));
            request.setTitle(getString(R.string.title_notification))
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setDescription(zip_name.trim())
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, zip_name.trim());
            queueID = downloadManager.enqueue(request);
            download_in_progress = true;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        downloadManager.remove(queueID);
        download_in_progress = false;
    }

    public class Receiver extends BroadcastReceiver {
        private NotificationManager manager;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                download_in_progress = false;
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(queueID);
                Cursor c = downloadManager.query(query);
                if (c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                        Intent notify = new Intent(lib.FLAG_ACTION_REBOOT);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notify, 0);
                        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        Notification.Builder builder = new Notification.Builder(getApplicationContext());
                        builder.setContentTitle(getString(R.string.message_downloadComplete))
                                .setSmallIcon(R.drawable.purity)
                                .setContentText(getString(R.string.message_clickInstall))
                                .setContentIntent(pendingIntent);
                        manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, builder.build());
                        try {
                            DownloadActivity activity = DownloadActivity.THIS;
                            activity.updateMessage(R.string.message_downloadComplete);
                        } catch (Exception ignored) {
                        }
                    } else {
                        try {
                            DownloadActivity activity = DownloadActivity.THIS;
                            activity.updateMessage(R.string.message_downloadAborted);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    try {
                        DownloadActivity activity = DownloadActivity.THIS;
                        activity.updateMessage(R.string.message_downloadAborted);
                    } catch (Exception ignored) {
                    }
                }
            } else if (action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
                Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } else if (action.equals(lib.FLAG_ACTION_REBOOT)) {
                if (manager != null)
                    manager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID);
                new Scripter() {
                    @Override
                    protected void onPostExecute(Void aVoid) {
                        Toast.makeText(getApplicationContext(), R.string.message_rebooting, Toast.LENGTH_LONG).show();
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                //Shell.SU.run("reboot recovery");
                                return null;
                            }
                        }.execute();
                    }
                }.execute(zip_name, OPTIONS);
            }
        }
    }

}
