package com.themike10452.purityu2d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import eu.chainfire.libsuperuser.Shell;


public class MainActivity extends Activity {

    private boolean firstRun;
    private TextView currentVersion, releaseDate, device;
    private String codename, dateTag;
    private Button check, downloadLatest;
    private Activity activity;
    private Spinner spinner;
    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstRun = true;
        getFilesDir();
        activity = this;
        (new File(Environment.getExternalStorageDirectory() + File.separator + lib.onPostInstallFolder)).mkdirs();
        if (!AutoCheckService.loop) {
            stopService(new Intent(this, AutoCheckService.class));
            startService(new Intent(this, AutoCheckService.class));
        }
        currentVersion = (TextView) findViewById(R.id.currentVersionDisplay);
        releaseDate = (TextView) findViewById(R.id.releaseDateDisplay);
        device = (TextView) findViewById(R.id.deviceDisplay);
        check = (Button) findViewById(R.id.btnCheck);
        spinner = ((Spinner) findViewById(R.id.spinner1));
        spinner.setAdapter(
                new ArrayAdapter<String>(
                        getApplicationContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        getResources().getStringArray(R.array.autocheck_ops)
                )
        );
        try {
            spinner.setSelection(
                    Integer.parseInt(
                            lib.shellOut(
                                    String.format("cat %s | grep %s", getFilesDir() + "/settings", "cycle="), "=", 1)
                                    .trim()
                    )
            );
        } catch (Exception ignored) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    Shell.SH.run(String.format("echo cycle=%s > %s", 0, getFilesDir() + "/settings"));
                    spinner.setSelection(0);
                    return null;
                }
            }.execute();
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, final int i, long l) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Shell.SH.run(String.format("echo cycle=%s > %s", i, getFilesDir() + "/settings"));
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if (firstRun) {
                            firstRun = !firstRun;
                        } else {
                            stopService(new Intent(activity, AutoCheckService.class));
                            if (i != 4) {
                                startService(new Intent(activity, AutoCheckService.class));
                            }
                        }
                    }
                }.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if ((new File(getFilesDir() + "/enable_developer").isFile()))
            findViewById(R.id.devBtn).setVisibility(View.VISIBLE);

        final LinearLayout opts = (LinearLayout) findViewById(R.id.hiddenOpts);
        opts.setVisibility(View.GONE);

        findViewById(R.id.btn_showMoreOptions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //initialize new buttons for first use
                if (downloadLatest == null) {
                    downloadLatest = (Button) findViewById(R.id.btn_downloadLatest);
                    downloadLatest.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (DownloadService.download_in_progress) {
                                Toast.makeText(getApplicationContext(), getString(R.string.message_failure_downloadInProgress), Toast.LENGTH_LONG).show();
                            } else {
                                check(false);
                            }
                        }
                    });
                }

                //

                switch (opts.getVisibility()) {
                    case View.VISIBLE:
                        opts.postOnAnimationDelayed(new Runnable() {
                            @Override
                            public void run() {
                                opts.setVisibility(View.GONE);
                            }
                        }, 300);
                        scaleView(opts, 1f, 0f);
                        ((Button) view).setText("More options");
                        break;
                    case View.GONE:
                    case View.INVISIBLE:
                    default:
                        ((Button) view).setText("Less options");
                        opts.setVisibility(View.VISIBLE);
                        scaleView(opts, 0f, 1f);
                }

            }
        });

    }

    public void scaleView(View v, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                1f, 1f,
                startScale, endScale,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
        anim.setFillAfter(true);
        anim.setDuration(300);
        v.startAnimation(anim);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                adjustSettings();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        thisActivity = this;
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
                            check(true);
                        }
                    }
                });
            }
        }.execute();
    }

    private void check(final boolean lookForUpdateOnly) {
        final String host_file = ((new File(getFilesDir() + "/enable_test")).exists()) ? lib.test_host : lib.emergency_host;
        final File file = new File(getFilesDir() + File.separator + "host");
        FileDownloader fileDownloader = new FileDownloader(this, host_file, file, false, true) {
            @Override
            protected void onPostExecute(Boolean successful) {
                super.onPostExecute(successful);
                if (successful) {
                    String tmp = host_file;
                    try {
                        String s = Shell.SH.run(String.format("cat %s", file.toString())).get(0);
                        if (s.contains("#no_thanks#"))
                            tmp = ((new File(getFilesDir() + "/enable_test")).exists()) ? lib.test_host : lib.host;
                    } catch (Exception ignored) {
                        tmp = ((new File(getFilesDir() + "/enable_test")).exists()) ? lib.test_host : lib.host;
                    }
                    final String hst = tmp;
                    FileDownloader downloader = new FileDownloader(thisActivity, hst, file, false, true) {
                        @Override
                        protected void onPostExecute(Boolean successful) {
                            super.onPostExecute(successful);
                            if (successful) process(file, lookForUpdateOnly);
                        }
                    };
                    downloader.execute();
                }
            }
        };
        fileDownloader.execute();
    }

    private void process(File file, boolean lookForUpdateOnly) {
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
                return;
            } else {
                String latest = line.substring(line.indexOf("=") + 1, line.indexOf(">>")).trim();
                if (lookForUpdateOnly) {
                    int i = latest.compareTo(dateTag);
                    if (i == 0) {
                        Toast.makeText(this, R.string.message_noUpdate, Toast.LENGTH_LONG).show();
                        return;
                    } else if (i < 0) {
                        Toast.makeText(this, getString(R.string.message_betaBuild), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            Intent intent = new Intent(this, DownloadActivity.class);
            intent.putExtra("0x0", line.split("=")[1]);
            if (!lookForUpdateOnly)
                intent.putExtra("custom_title", "Download the latest ROM zip");
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, R.string.message_failure_error, Toast.LENGTH_LONG).show();
        }
    }

    public void go(View v) {
        startService(new Intent(this, AutoCheckService.class));
    }

    public void adjustSettings() {
        Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT).show();
    }

}
