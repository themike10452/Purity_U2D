package com.themike10452.purityu2d;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Mike on 4/22/2014.
 */
public class FileDownloader extends AsyncTask<String, Integer, Boolean> {

    private boolean interruptDownload, quiet, indeterminate, cancelable;
    private HttpURLConnection connection1;
    private ProgressDialog dialog;
    private String sUrl, message, title;
    private File dFile;
    private int icon;
    private Context context;

    FileDownloader(Context context, String url, File file, boolean quiet, boolean indeterminate) {
        this.context = context;
        this.indeterminate = indeterminate;
        this.quiet = quiet;
        sUrl = url;
        dFile = file;
        message = null;
        title = null;
        icon = -1;
        cancelable = true;
        interruptDownload = false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!quiet) {
            dialog = new ProgressDialog(context);
            if (message == null)
                dialog.setMessage(context.getString(R.string.message_pleaseWait));
            else
                dialog.setMessage(message);
            if (title != null)
                dialog.setTitle(title);
            if (icon != -1)
                dialog.setIcon(icon);
            dialog.setIndeterminate(indeterminate);
            dialog.setCancelable(cancelable);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    onDialogCanceled();
                }
            });
            dialog.show();
        }
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            connection1 = (HttpURLConnection) (new URL(sUrl)).openConnection();
            FileOutputStream fileOutput = new FileOutputStream(dFile);
            InputStream inputStream = connection1.getInputStream();
            int totalLength = connection1.getContentLength();
            int downloadedLength = 0;
            byte[] buffer = new byte[1024];
            int bufferLength;
            while ((bufferLength = inputStream.read(buffer)) > 0 && !interruptDownload) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedLength += bufferLength;
                if (!indeterminate)
                    publishProgress(downloadedLength, totalLength);
            }
            inputStream.close();
            fileOutput.close();
            connection1.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (!quiet && !(dialog == null)) {
            dialog.setMax(100);
            dialog.setProgress((values[0] / values[1]) * 100);
        }
    }

    @Override
    protected void onPostExecute(Boolean successful) {
        super.onPostExecute(successful);
        if (!(dialog == null))
            dialog.dismiss();
        if (!quiet && !successful && !interruptDownload) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(true).setTitle(":(").setMessage(R.string.message_failure_serviceNA).show();
        }
    }

    public void onDialogCanceled() {
        stop();
        this.cancel(true);
    }

    public void stop() {
        interruptDownload = true;
    }

    public FileDownloader setTitle(String title) {
        this.title = title;
        return this;
    }

    public FileDownloader setMessage(String message) {
        this.message = message;
        return this;
    }

    public FileDownloader setIcon(int resId) {
        icon = resId;
        return this;
    }

    public FileDownloader setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

}
