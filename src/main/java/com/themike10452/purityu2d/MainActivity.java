package com.themike10452.purityu2d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class MainActivity extends Activity {

    private TextView currentVersion, releaseDate, device;
    private String codename, dateTag;
    private Button check;
    private boolean killedByMaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getFilesDir();
        (new File(Environment.getExternalStorageDirectory() + File.separator + lib.onPostInstallFolder)).mkdirs();
        currentVersion = (TextView) findViewById(R.id.currentVersionDisplay);
        releaseDate = (TextView) findViewById(R.id.releaseDateDisplay);
        device = (TextView) findViewById(R.id.deviceDisplay);
        check = (Button) findViewById(R.id.btnCheck);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new AsyncTask<Void, Void, Void>() {
            private String cv
                    ,
                    rd
                    ,
                    dev;

            @Override
            protected Void doInBackground(Void... voids) {
                cv = lib.shellOut(String.format("cat %s | grep %s ", lib.buildProp, lib.versionPropTag), "=", 1);
                try {
                    dateTag = cv.split("\\.")[2];
                    cv += String.format(" (%s)", dateTag);
                } catch (IndexOutOfBoundsException e) {
                    dateTag = "n/a";
                }
                rd = lib.shellOut(String.format("cat %s | grep %s ", lib.buildProp, lib.buildDatePropTag), "=", 1);
                String p1 = lib.shellOut(String.format("cat %s | grep %s", lib.buildProp, lib.modelPropTag), "=", 1);
                codename = lib.shellOut(String.format("cat %s | grep %s", lib.buildProp, lib.devicePropTag), "=", 1)
                        .toLowerCase()
                        .trim();
                dev = String.format("%s (%s)", p1, codename);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                currentVersion.setText(cv);
                releaseDate.setText(rd);
                device.setText(dev);
                check.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (DownloadService.download_in_progress) {
                            Toast.makeText(getApplicationContext(), R.string.message_failure_downloadInProgress, Toast.LENGTH_SHORT).show();
                        } else {
                            check();
                        }
                    }
                });
            }
        }.execute();
    }

    private void check() {
        final File file = new File(getFilesDir() + File.separator + "host");
        FileDownloader downloader = new FileDownloader(this, lib.host, file, false, true) {
            @Override
            protected void onPostExecute(Boolean successful) {
                super.onPostExecute(successful);
                if (successful) process(file);
            }
        };
        downloader.execute();
    }

    private void process(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(codename + "=")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format(getString(R.string.message_deviceNotSupported), codename))
                        .setCancelable(true)
                        .show();
            } else {
                String latest = line.substring(line.indexOf("=") + 1, line.indexOf(">>")).trim();
                int i = latest.compareTo(dateTag);
                if (i == 0) {
                    Toast.makeText(this, R.string.message_noUpdate, Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(this, DownloadActivity.class);
                    intent.putExtra("0x0", line.split("=")[1]);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.message_failure_error, Toast.LENGTH_LONG).show();
        }
    }

}
